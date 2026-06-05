Place humanoid GLB avatar files here:

  male.glb    - glTF 2.0 humanoid rig (male body)
  female.glb  - glTF 2.0 humanoid rig (female body)

Requirements:
  - glTF 2.0 binary format (.glb)
  - Humanoid skeleton with standard bone names (e.g. Mixamo or Rigify rig)
  - File size: ideally < 10 MB each

Bone name → COCO keypoint mapping (implement in AvatarRenderer.driveSkeletonFromPose):
  Bone "Hips"              ← leftHip + rightHip midpoint
  Bone "LeftArm"           ← leftShoulder → leftElbow
  Bone "LeftForeArm"       ← leftElbow → leftWrist
  Bone "RightArm"          ← rightShoulder → rightElbow
  Bone "RightForeArm"      ← rightElbow → rightWrist
  Bone "LeftUpLeg"         ← leftHip → leftKnee
  Bone "LeftLeg"           ← leftKnee → leftAnkle
  Bone "RightUpLeg"        ← rightHip → rightKnee
  Bone "RightLeg"          ← rightKnee → rightAnkle
  Bone "Head"              ← nose
