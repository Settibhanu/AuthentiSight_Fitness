#include "inference_helper.h"

#include <opencv2/imgproc.hpp>
#include <algorithm>
#include <cmath>
#include <android/log.h>

#define LOG_TAG "InferenceHelper"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static constexpr int HEATMAP_W = 48;
static constexpr int HEATMAP_H = 64;
static constexpr int NUM_KP    = 17;

// ── Pre-processing ─────────────────────────────────────────────────────────

cv::Mat cropAndWarp(const cv::Mat& frame,
                    float x1, float y1, float x2, float y2,
                    int targetW, int targetH)
{
    // Add 25% padding around the detected bounding box
    float cx = (x1 + x2) / 2.0f;
    float cy = (y1 + y2) / 2.0f;
    float w  = (x2 - x1) * 1.25f;
    float h  = (y2 - y1) * 1.25f;

    // Clamp to frame bounds
    float px1 = std::max(0.0f, cx - w / 2.0f);
    float py1 = std::max(0.0f, cy - h / 2.0f);
    float px2 = std::min(static_cast<float>(frame.cols), cx + w / 2.0f);
    float py2 = std::min(static_cast<float>(frame.rows), cy + h / 2.0f);

    cv::Point2f src[3] = {
        { px1, py1 },
        { px2, py1 },
        { px1, py2 }
    };
    cv::Point2f dst[3] = {
        { 0.0f,              0.0f              },
        { (float)targetW,    0.0f              },
        { 0.0f,              (float)targetH    }
    };

    cv::Mat M = cv::getAffineTransform(src, dst);
    cv::Mat warped;
    cv::warpAffine(frame, warped, M, cv::Size(targetW, targetH),
                   cv::INTER_LINEAR, cv::BORDER_CONSTANT, cv::Scalar(128, 128, 128));
    return warped;
}

cv::Mat normalizeForHRNet(const cv::Mat& rgbCrop)
{
    cv::Mat floatImg;
    rgbCrop.convertTo(floatImg, CV_32FC3, 1.0f / 255.0f);

    std::vector<cv::Mat> channels(3);
    cv::split(floatImg, channels);

    // ImageNet mean / std normalization
    channels[0] = (channels[0] - 0.485f) / 0.229f;   // R
    channels[1] = (channels[1] - 0.456f) / 0.224f;   // G
    channels[2] = (channels[2] - 0.406f) / 0.225f;   // B

    cv::Mat normalized;
    cv::merge(channels, normalized);
    return normalized;
}

// ── NMS ───────────────────────────────────────────────────────────────────

float computeIoU(const BoundingBox& a, const BoundingBox& b)
{
    float interX1 = std::max(a.x1, b.x1);
    float interY1 = std::max(a.y1, b.y1);
    float interX2 = std::min(a.x2, b.x2);
    float interY2 = std::min(a.y2, b.y2);

    float interW = std::max(0.0f, interX2 - interX1);
    float interH = std::max(0.0f, interY2 - interY1);
    float interArea = interW * interH;

    float aArea = (a.x2 - a.x1) * (a.y2 - a.y1);
    float bArea = (b.x2 - b.x1) * (b.y2 - b.y1);
    float unionArea = aArea + bArea - interArea;

    return (unionArea > 0.0f) ? interArea / unionArea : 0.0f;
}

std::vector<BoundingBox> applyNMS(const std::vector<BoundingBox>& boxes, float iouThresh)
{
    if (boxes.empty()) return {};

    // Sort by confidence descending
    std::vector<int> indices(boxes.size());
    std::iota(indices.begin(), indices.end(), 0);
    std::sort(indices.begin(), indices.end(),
              [&](int a, int b) { return boxes[a].confidence > boxes[b].confidence; });

    std::vector<bool> suppressed(boxes.size(), false);
    std::vector<BoundingBox> result;

    for (int i = 0; i < (int)indices.size(); ++i) {
        int idx = indices[i];
        if (suppressed[idx]) continue;
        result.push_back(boxes[idx]);
        for (int j = i + 1; j < (int)indices.size(); ++j) {
            int jdx = indices[j];
            if (!suppressed[jdx] && computeIoU(boxes[idx], boxes[jdx]) > iouThresh) {
                suppressed[jdx] = true;
            }
        }
    }
    return result;
}

// ── Heatmap decoding ──────────────────────────────────────────────────────

std::vector<Keypoint> decodeHeatmaps(
    const float* heatmaps,
    float boxX1, float boxY1, float boxX2, float boxY2,
    int /*imageW*/, int /*imageH*/)
{
    std::vector<Keypoint> keypoints;
    keypoints.reserve(NUM_KP);

    for (int k = 0; k < NUM_KP; ++k) {
        const float* hm = heatmaps + k * HEATMAP_W * HEATMAP_H;
        float maxVal = -1e9f;
        int   maxX = 0, maxY = 0;

        for (int y = 0; y < HEATMAP_H; ++y) {
            for (int x = 0; x < HEATMAP_W; ++x) {
                float v = hm[y * HEATMAP_W + x];
                if (v > maxVal) {
                    maxVal = v; maxX = x; maxY = y;
                }
            }
        }

        // Map heatmap peak back to image pixel space via the padded crop box
        float boxW = boxX2 - boxX1;
        float boxH = boxY2 - boxY1;
        float px = boxX1 + (maxX + 0.5f) / HEATMAP_W * boxW;
        float py = boxY1 + (maxY + 0.5f) / HEATMAP_H * boxH;

        keypoints.push_back({ px, py, maxVal });
    }
    return keypoints;
}
