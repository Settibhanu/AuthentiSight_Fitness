#pragma once

#include <opencv2/core.hpp>
#include <vector>
#include <array>

/**
 * inference_helper.h
 * Utility functions shared between inference.cpp and snpe_helper.cpp.
 */

// ── Pre-processing ─────────────────────────────────────────────────────────

/**
 * Affine-warp a person crop from the full frame to the HRNet input size.
 * Adds 25% padding around the bounding box before warping.
 *
 * @param frame    Full RGBA or RGB frame (any size)
 * @param x1,y1   Top-left corner of bounding box (pixel coords)
 * @param x2,y2   Bottom-right corner of bounding box (pixel coords)
 * @param targetW  Output width  (192 for HRNet)
 * @param targetH  Output height (256 for HRNet)
 * @return         Cropped and warped RGB mat of size targetW × targetH
 */
cv::Mat cropAndWarp(const cv::Mat& frame,
                    float x1, float y1, float x2, float y2,
                    int targetW, int targetH);

/**
 * Normalise a 256×192 RGB crop to HRNet's expected float tensor.
 * mean = [0.485, 0.456, 0.406], std = [0.229, 0.224, 0.225]
 * Output: CV_32FC3 mat, CHW layout when converted with cv::dnn::blobFromImage.
 */
cv::Mat normalizeForHRNet(const cv::Mat& rgbCrop);

// ── Post-processing ────────────────────────────────────────────────────────

struct BoundingBox {
    float x1, y1, x2, y2, confidence;
};

struct Keypoint {
    float x, y, confidence;
};

/**
 * Non-maximum suppression.
 * @param boxes     Input boxes (already filtered by score threshold)
 * @param iouThresh IoU threshold for suppression (e.g. 0.45)
 * @return          Surviving boxes after NMS
 */
std::vector<BoundingBox> applyNMS(const std::vector<BoundingBox>& boxes, float iouThresh);

/**
 * Compute Intersection-over-Union for two boxes.
 */
float computeIoU(const BoundingBox& a, const BoundingBox& b);

/**
 * Decode 17 heatmaps (each HEATMAP_H × HEATMAP_W) back to image keypoints.
 * @param heatmaps  Float array of shape [17 × HEATMAP_H × HEATMAP_W]
 * @param boxX1, boxY1, boxX2, boxY2  Original (padded) person crop in image space
 * @param imageW, imageH  Full image dimensions
 * @return Vector of 17 keypoints in image pixel space
 */
std::vector<Keypoint> decodeHeatmaps(
    const float* heatmaps,
    float boxX1, float boxY1, float boxX2, float boxY2,
    int imageW, int imageH);
