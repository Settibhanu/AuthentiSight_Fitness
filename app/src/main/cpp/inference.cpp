#include <jni.h>
#include <string>
#include <vector>
#include <memory>
#include <android/log.h>

#include "snpe_helper.h"
#include "inference_helper.h"

#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>

#define LOG_TAG "SNPEInference"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ── Global SNPE network handles ──────────────────────────────────────────────
static std::unique_ptr<zdl::SNPE::SNPE> g_snpe_bb;    // YOLO-NAS-S person detector
static std::unique_ptr<zdl::SNPE::SNPE> g_snpe_pose;  // HRNet-W32 pose estimator

static constexpr float CONF_THRESHOLD = 0.20f;
static constexpr float NMS_THRESHOLD  = 0.45f;
static constexpr int   YOLO_SIZE      = 320;
static constexpr int   HRNET_W        = 192;
static constexpr int   HRNET_H        = 256;
static constexpr int   NUM_KP         = 17;

// ── JNI: buildNetworkBB ──────────────────────────────────────────────────────
extern "C" JNIEXPORT jboolean JNICALL
Java_com_fitness_snapapp_ai_qairt_SNPEHelper_buildNetworkBB(
    JNIEnv* env, jobject /*thiz*/,
    jbyteArray dlcBuf, jint size, jchar runtime)
{
    jbyte* raw = env->GetByteArrayElements(dlcBuf, nullptr);
    bool ok = buildSNPENetwork(
        reinterpret_cast<const uint8_t*>(raw),
        static_cast<size_t>(size),
        static_cast<char>(runtime),
        g_snpe_bb
    );
    env->ReleaseByteArrayElements(dlcBuf, raw, JNI_ABORT);
    LOGI("buildNetworkBB: %s", ok ? "OK" : "FAIL");
    return ok ? JNI_TRUE : JNI_FALSE;
}

// ── JNI: buildNetworkPose ────────────────────────────────────────────────────
extern "C" JNIEXPORT jboolean JNICALL
Java_com_fitness_snapapp_ai_qairt_SNPEHelper_buildNetworkPose(
    JNIEnv* env, jobject /*thiz*/,
    jbyteArray dlcBuf, jint size, jchar runtime)
{
    jbyte* raw = env->GetByteArrayElements(dlcBuf, nullptr);
    bool ok = buildSNPENetwork(
        reinterpret_cast<const uint8_t*>(raw),
        static_cast<size_t>(size),
        static_cast<char>(runtime),
        g_snpe_pose
    );
    env->ReleaseByteArrayElements(dlcBuf, raw, JNI_ABORT);
    LOGI("buildNetworkPose: %s", ok ? "OK" : "FAIL");
    return ok ? JNI_TRUE : JNI_FALSE;
}

// ── Helpers: run YOLO-NAS inference ─────────────────────────────────────────

/**
 * Preprocess one frame for YOLO-NAS-S:
 *   RGBA → RGB → resize to 320×320 → normalise to [0,1] → flatten to CHW float array
 */
static std::vector<float> preprocessForYolo(const cv::Mat& rgba, int width, int height)
{
    cv::Mat rgb;
    cv::cvtColor(rgba, rgb, cv::COLOR_RGBA2RGB);
    cv::Mat resized;
    cv::resize(rgb, resized, cv::Size(YOLO_SIZE, YOLO_SIZE));
    cv::Mat floatImg;
    resized.convertTo(floatImg, CV_32FC3, 1.0f / 255.0f);

    // Convert HWC → CHW (SNPE expects CHW layout)
    std::vector<cv::Mat> channels(3);
    cv::split(floatImg, channels);
    std::vector<float> chw;
    chw.reserve(3 * YOLO_SIZE * YOLO_SIZE);
    for (auto& ch : channels)
        chw.insert(chw.end(), (float*)ch.datastart, (float*)ch.dataend);
    return chw;
}

/**
 * Parse raw YOLO-NAS output tensors into BoundingBox structs.
 * Decodes boxes in [cx, cy, w, h] format scaled back to original image size.
 */
static std::vector<BoundingBox> decodeYoloOutput(
    const float* boxes, size_t numBoxes,
    const float* scores, size_t /*numClasses*/,
    int origW, int origH)
{
    std::vector<BoundingBox> detections;
    float scaleX = static_cast<float>(origW) / YOLO_SIZE;
    float scaleY = static_cast<float>(origH) / YOLO_SIZE;

    for (size_t i = 0; i < numBoxes; ++i) {
        float score = scores[i];
        if (score < CONF_THRESHOLD) continue;

        // YOLO-NAS-S outputs boxes as [x1, y1, x2, y2] in normalised coords
        float x1 = boxes[i * 4 + 0] * scaleX;
        float y1 = boxes[i * 4 + 1] * scaleY;
        float x2 = boxes[i * 4 + 2] * scaleX;
        float y2 = boxes[i * 4 + 3] * scaleY;
        detections.push_back({ x1, y1, x2, y2, score });
    }
    return detections;
}

// ── JNI: runInference ────────────────────────────────────────────────────────
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_fitness_snapapp_ai_qairt_SNPEHelper_runInference(
    JNIEnv* env, jobject /*thiz*/,
    jbyteArray rgbaBytes, jint width, jint height)
{
    auto empty = [&]() { return env->NewFloatArray(0); };

    if (!g_snpe_bb || !g_snpe_pose) {
        LOGE("Networks not initialised — call buildNetworkBB/buildNetworkPose first");
        return empty();
    }

    // ── Stage 1: Build OpenCV Mat from RGBA byte array ──────────────────────
    jbyte* rgba = env->GetByteArrayElements(rgbaBytes, nullptr);
    cv::Mat frame(height, width, CV_8UC4, rgba);

    // Pre-process for YOLO-NAS
    auto yoloInput = preprocessForYolo(frame, width, height);

    // ── Stage 2: YOLO-NAS inference ─────────────────────────────────────────
    // Set up input user buffer
    auto& inputNames = g_snpe_bb->getInputTensorNames();
    zdl::DlSystem::UserBufferMap inputMap, outputMap;

    std::vector<std::unique_ptr<zdl::DlSystem::IUserBuffer>> inBufs, outBufs;
    std::vector<std::vector<float>> outData;

    // Input buffer
    {
        const char* inName = inputNames.at(0);
        zdl::DlSystem::TensorShape inStrides({ sizeof(float) });
        auto enc = std::make_unique<zdl::DlSystem::UserBufferEncoding>(
            zdl::DlSystem::UserBufferEncoding::ElementType_t::FLOAT);
        auto buf = zdl::DlSystem::IUserBufferFactory::createUserBuffer(
            yoloInput.data(),
            yoloInput.size() * sizeof(float),
            inStrides, enc.get());
        inputMap.add(inName, buf.get());
        inBufs.push_back(std::move(buf));
    }

    // Output buffers (boxes + scores)
    auto& outputNames = g_snpe_bb->getOutputTensorNames();
    for (size_t i = 0; i < outputNames.size(); ++i) {
        const char* outName = outputNames.at(i);
        auto attrs = g_snpe_bb->getInputOutputBufferAttributes(outName);
        auto& dims = attrs->getDims();
        size_t total = 1;
        for (size_t d = 0; d < dims.rank(); ++d) total *= dims[d];

        outData.emplace_back(total, 0.0f);
        zdl::DlSystem::TensorShape strides({ sizeof(float) });
        auto enc = std::make_unique<zdl::DlSystem::UserBufferEncoding>(
            zdl::DlSystem::UserBufferEncoding::ElementType_t::FLOAT);
        auto buf = zdl::DlSystem::IUserBufferFactory::createUserBuffer(
            outData.back().data(), total * sizeof(float), strides, enc.get());
        outputMap.add(outName, buf.get());
        outBufs.push_back(std::move(buf));
    }

    bool bbOk = g_snpe_bb->execute(inputMap, outputMap);
    if (!bbOk) {
        LOGE("YOLO-NAS execute() failed");
        env->ReleaseByteArrayElements(rgbaBytes, rgba, JNI_ABORT);
        return empty();
    }

    // Parse YOLO output: first tensor = boxes [N,4], second = scores [N]
    const float* rawBoxes  = outData[0].data();
    const float* rawScores = outData[1].data();
    size_t numCandidates = outData[0].size() / 4;

    auto detections = decodeYoloOutput(rawBoxes, numCandidates, rawScores, 1, width, height);
    detections = applyNMS(detections, NMS_THRESHOLD);

    env->ReleaseByteArrayElements(rgbaBytes, rgba, JNI_ABORT);

    if (detections.empty()) return empty();

    // ── Stage 3–5: For each person: crop → HRNet → keypoints ────────────────
    cv::Mat rgbFull;
    {
        // Re-acquire — we released above
        jbyte* rgbaAgain = env->GetByteArrayElements(rgbaBytes, nullptr);
        cv::Mat frameFull(height, width, CV_8UC4, rgbaAgain);
        cv::cvtColor(frameFull, rgbFull, cv::COLOR_RGBA2RGB);
        env->ReleaseByteArrayElements(rgbaBytes, rgbaAgain, JNI_ABORT);
    }

    std::vector<float> result;
    result.push_back(static_cast<float>(detections.size()));

    for (auto& det : detections) {
        result.push_back(det.x1);
        result.push_back(det.y1);
        result.push_back(det.x2);
        result.push_back(det.y2);

        // Crop & warp person region to 192×256
        cv::Mat crop = cropAndWarp(rgbFull, det.x1, det.y1, det.x2, det.y2, HRNET_W, HRNET_H);
        cv::Mat normalised = normalizeForHRNet(crop);

        // CHW layout for HRNet
        std::vector<cv::Mat> chans(3);
        cv::split(normalised, chans);
        std::vector<float> hrnetInput;
        hrnetInput.reserve(3 * HRNET_W * HRNET_H);
        for (auto& ch : chans)
            hrnetInput.insert(hrnetInput.end(), (float*)ch.datastart, (float*)ch.dataend);

        // HRNet input/output buffers
        zdl::DlSystem::UserBufferMap hInMap, hOutMap;
        std::vector<std::unique_ptr<zdl::DlSystem::IUserBuffer>> hInBufs, hOutBufs;
        std::vector<std::vector<float>> hOutData;

        {
            const char* inName = g_snpe_pose->getInputTensorNames().at(0);
            zdl::DlSystem::TensorShape strides({ sizeof(float) });
            auto enc = std::make_unique<zdl::DlSystem::UserBufferEncoding>(
                zdl::DlSystem::UserBufferEncoding::ElementType_t::FLOAT);
            auto buf = zdl::DlSystem::IUserBufferFactory::createUserBuffer(
                hrnetInput.data(), hrnetInput.size() * sizeof(float), strides, enc.get());
            hInMap.add(inName, buf.get());
            hInBufs.push_back(std::move(buf));
        }

        auto& hOutNames = g_snpe_pose->getOutputTensorNames();
        for (size_t i = 0; i < hOutNames.size(); ++i) {
            const char* oName = hOutNames.at(i);
            auto attrs = g_snpe_pose->getInputOutputBufferAttributes(oName);
            auto& dims = attrs->getDims();
            size_t total = 1;
            for (size_t d = 0; d < dims.rank(); ++d) total *= dims[d];
            hOutData.emplace_back(total, 0.0f);
            zdl::DlSystem::TensorShape strides({ sizeof(float) });
            auto enc = std::make_unique<zdl::DlSystem::UserBufferEncoding>(
                zdl::DlSystem::UserBufferEncoding::ElementType_t::FLOAT);
            auto buf = zdl::DlSystem::IUserBufferFactory::createUserBuffer(
                hOutData.back().data(), total * sizeof(float), strides, enc.get());
            hOutMap.add(oName, buf.get());
            hOutBufs.push_back(std::move(buf));
        }

        bool poseOk = g_snpe_pose->execute(hInMap, hOutMap);
        if (!poseOk) {
            LOGE("HRNet execute() failed for one person — skipping keypoints");
            for (int k = 0; k < NUM_KP; ++k) {
                result.push_back(0); result.push_back(0); result.push_back(0);
            }
            continue;
        }

        // Decode 17 heatmaps from the first output tensor
        // HRNet output: [1, 17, 64, 48]
        const float* heatmaps = hOutData[0].data();
        auto kps = decodeHeatmaps(heatmaps, det.x1, det.y1, det.x2, det.y2, width, height);

        for (auto& kp : kps) {
            result.push_back(kp.x);
            result.push_back(kp.y);
            result.push_back(kp.confidence);
        }
    }

    // ── Stage 6: Return float array to Kotlin — frame is discarded here ──────
    jfloatArray out = env->NewFloatArray(static_cast<jsize>(result.size()));
    env->SetFloatArrayRegion(out, 0, static_cast<jsize>(result.size()), result.data());
    return out;
}

// ── JNI: destroy ─────────────────────────────────────────────────────────────
extern "C" JNIEXPORT void JNICALL
Java_com_fitness_snapapp_ai_qairt_SNPEHelper_destroy(JNIEnv* /*env*/, jobject /*thiz*/)
{
    g_snpe_bb.reset();
    g_snpe_pose.reset();
    LOGI("SNPE networks destroyed");
}
