package com.fitness.snapapp.core.constants

object AppConstants {
    // AI model input dimensions
    const val YOLO_INPUT_SIZE = 320
    const val HRNET_INPUT_W = 192
    const val HRNET_INPUT_H = 256
    const val HRNET_KEYPOINT_COUNT = 17

    // Inference thresholds
    const val DETECTION_CONFIDENCE_THRESHOLD = 0.20f
    const val NMS_IOU_THRESHOLD = 0.45f

    // Posture angle thresholds (degrees)
    const val SQUAT_UP_HIP_ANGLE   = 160f
    const val SQUAT_DOWN_HIP_ANGLE = 90f
    const val PUSHUP_ELBOW_DOWN_ANGLE = 90f
    const val PUSHUP_ELBOW_UP_ANGLE   = 160f
    const val BACK_STRAIGHT_MIN = 160f
    const val KNEE_FORWARD_MAX  = 85f

    // Performance
    const val TARGET_FRAME_LATENCY_MS = 40L

    // File paths (external files dir sub-dirs)
    const val VIDEOS_DIR = "videos"
    const val MUSIC_DIR  = "music"
    const val MODELS_DIR = "models"

    // Avatar asset names (inside assets/avatars/)
    const val AVATAR_MALE   = "avatars/male.glb"
    const val AVATAR_FEMALE = "avatars/female.glb"

    // DLC asset file names
    const val DLC_YOLO  = "Quant_yoloNas_s_320.dlc"
    const val DLC_HRNET = "hrnet_axis_int8.dlc"

    // Exercise string keys (match ExerciseCounter.ExerciseType names)
    const val EXERCISE_SQUAT         = "SQUATS"
    const val EXERCISE_PUSHUP        = "PUSH_UPS"
    const val EXERCISE_SITUP         = "SIT_UPS"
    const val EXERCISE_LUNGE         = "LUNGES"
    const val EXERCISE_JUMPING_JACK  = "JUMPING_JACKS"
    const val EXERCISE_BURPEE        = "BURPEES"
    const val EXERCISE_PLANK         = "PLANK"

    // DataStore preference keys
    const val PREF_RUNTIME         = "pref_runtime"       // 'C', 'G', or 'D'
    const val PREF_AVATAR_GENDER   = "pref_avatar_gender" // "MALE" or "FEMALE"
    const val PREF_ONBOARDING_DONE = "pref_onboarding_done"
}
