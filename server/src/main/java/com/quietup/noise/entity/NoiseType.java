package com.quietup.noise.entity;

public enum NoiseType {
    FOOTSTEPS("발걸음 소리가 반복적으로 들리고 있습니다."),
    FURNITURE("가구를 움직이는 듯한 소리가 반복적으로 들리고 있습니다."),
    MUSIC("음악이나 음향기기 소리가 반복적으로 들리고 있습니다."),
    CONSTRUCTION("공사나 작업으로 추정되는 소리가 반복적으로 들리고 있습니다."),
    PET("반려동물로 추정되는 소리가 반복적으로 들리고 있습니다."),
    OTHER("생활 소음이 반복적으로 들리고 있습니다.");

    private final String description;

    NoiseType(String description) {
        this.description = description;
    }

    public String displayMessage(NoiseDirection direction) {
        return "%s 방향에서 %s 가능하시다면 잠시 확인 부탁드립니다."
                .formatted(direction.directionLabel(), description);
    }
}
