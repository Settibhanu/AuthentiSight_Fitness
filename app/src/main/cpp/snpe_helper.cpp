#include "snpe_helper.h"
#include <android/log.h>

#define LOG_TAG "SNPEHelper"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ── Map runtime char to SNPE enum ─────────────────────────────────────────
zdl::DlSystem::Runtime_t charToRuntime(char r)
{
    switch (r) {
        case 'D': return zdl::DlSystem::Runtime_t::DSP;
        case 'G': return zdl::DlSystem::Runtime_t::GPU;
        default:  return zdl::DlSystem::Runtime_t::CPU;
    }
}

// ── Build one SNPE network ────────────────────────────────────────────────
bool buildSNPENetwork(
    const uint8_t* dlcData,
    size_t         dlcSize,
    char           runtimeChar,
    std::unique_ptr<zdl::SNPE::SNPE>& outSnpe)
{
    // Open DLC container from memory buffer
    auto container = zdl::DlContainer::IDlContainer::open(
        zdl::DlSystem::StringList{},
        dlcData,
        dlcSize
    );
    if (!container) {
        LOGE("IDlContainer::open failed");
        return false;
    }

    zdl::DlSystem::Runtime_t primaryRuntime = charToRuntime(runtimeChar);

    // Build a fallback runtime list: primary → GPU (if DSP) → CPU
    zdl::DlSystem::RuntimeList runtimeList;
    runtimeList.add(primaryRuntime);
    if (primaryRuntime == zdl::DlSystem::Runtime_t::DSP)
        runtimeList.add(zdl::DlSystem::Runtime_t::GPU);
    runtimeList.add(zdl::DlSystem::Runtime_t::CPU);

    auto builder = zdl::SNPE::SNPEBuilder(container.get());
    outSnpe = builder
        .setPerformanceProfile(zdl::DlSystem::PerformanceProfile_t::BURST)
        .setExecutionPriorityHint(zdl::DlSystem::ExecutionPriorityHint_t::HIGH)
        .setRuntimeProcessorOrder(runtimeList)
        .setUseUserSuppliedBuffers(true)
        .build();

    if (!outSnpe) {
        LOGE("SNPEBuilder::build() returned null");
        return false;
    }

    LOGI("Network built on runtime '%c' (with fallbacks)", runtimeChar);
    return true;
}

// ── User buffer helpers ────────────────────────────────────────────────────

/**
 * Create a UserBufferMap from a float vector for a network's input/output layer.
 * This is the zero-copy path: SNPE reads/writes directly into our vector.
 */
static zdl::DlSystem::UserBufferMap makeBufferMap(
    zdl::SNPE::SNPE* snpe,
    bool isInput,
    std::vector<std::unique_ptr<zdl::DlSystem::IUserBuffer>>& bufferHandles,
    std::vector<std::vector<float>>& bufferData)
{
    zdl::DlSystem::UserBufferMap bufMap;
    auto& names = isInput
        ? snpe->getInputTensorNames()
        : snpe->getOutputTensorNames();

    for (size_t i = 0; i < names.size(); ++i) {
        const char* name = names.at(i);

        // Get tensor shape
        auto dims = snpe->getInputOutputBufferAttributes(name)->getDims();
        size_t totalElems = 1;
        for (size_t d = 0; d < dims.rank(); ++d) totalElems *= dims[d];

        bufferData.emplace_back(totalElems);

        zdl::DlSystem::TensorShape strides({ sizeof(float) });
        auto encoding = std::make_unique<zdl::DlSystem::UserBufferEncoding>(
            zdl::DlSystem::UserBufferEncoding::ElementType_t::FLOAT);

        auto buf = zdl::DlSystem::IUserBufferFactory::createUserBuffer(
            bufferData.back().data(),
            totalElems * sizeof(float),
            strides,
            encoding.get()
        );
        bufMap.add(name, buf.get());
        bufferHandles.push_back(std::move(buf));
    }
    return bufMap;
}
