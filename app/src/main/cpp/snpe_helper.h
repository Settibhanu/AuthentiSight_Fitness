#pragma once

#include <memory>
#include <vector>
#include <string>

// SNPE / QAIRT headers (copied by resolveDependencies.sh)
#include "SNPE/SNPE.hpp"
#include "SNPE/SNPEFactory.hpp"
#include "SNPE/SNPEBuilder.hpp"
#include "DlContainer/IDlContainer.hpp"
#include "DlSystem/DlEnums.hpp"
#include "DlSystem/TensorShape.hpp"
#include "DlSystem/IUserBuffer.hpp"
#include "DlSystem/UserBufferMap.hpp"
#include "DlSystem/IUserBufferFactory.hpp"

/**
 * Build a single SNPE network from raw DLC bytes.
 *
 * @param dlcData      Pointer to DLC file bytes
 * @param dlcSize      Number of bytes
 * @param runtimeChar  'C' = CPU, 'G' = GPU, 'D' = DSP/HTP
 * @param outSnpe      Output — the constructed SNPE instance
 * @return true on success
 */
bool buildSNPENetwork(
    const uint8_t* dlcData,
    size_t         dlcSize,
    char           runtimeChar,
    std::unique_ptr<zdl::SNPE::SNPE>& outSnpe);

/**
 * Map a runtime character to a SNPE Runtime_t enum.
 */
zdl::DlSystem::Runtime_t charToRuntime(char r);
