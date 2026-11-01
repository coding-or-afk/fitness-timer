# Fitness Timer

운동할 때 쓰는 간단한 안드로이드 카운트다운 타이머입니다. 분/초를 고르고 시작하면, 시간이 다 됐을 때 복싱 벨이 울립니다.

## 기능

- 분(0–99)과 초(0–59)를 휠로 선택해 최대 99:59까지 설정
- 시작 / 일시정지 / 계속 / 초기화
- 타이머가 도는 동안 화면이 꺼지지 않음
- 남은 시간을 원형 진행 링으로 표시
- 시간이 끝나면 복싱 벨 소리 재생
- 오른쪽 위 버튼으로 라이트/다크 모드 전환 (처음에는 시스템 설정을 따르고, 고른 모드는 저장됨)
- 세로 화면 고정

## 사용법

1. 분과 초 휠을 돌려 시간을 맞춥니다.
2. **시작**을 누르면 카운트다운이 시작되고, 시간 선택 휠은 숨겨집니다.
3. 도중에 **일시정지**를 누르면 멈추고, **계속**을 누르면 남은 시간부터 다시 진행합니다.
4. **초기화**를 누르면 타이머가 멈추고 휠에 설정한 시간으로 돌아갑니다. 울리고 있는 벨도 멈춥니다.

## 요구 사항

- Android 7.0 (API 24) 이상
- 빌드: JDK 17, Android SDK 35

## 빌드 및 설치

Android Studio에서 프로젝트 폴더를 열고 실행하거나, 명령줄에서 Gradle Wrapper를 사용합니다.

```sh
# 디버그 APK 빌드 → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleDebug

# USB로 연결된 기기나 에뮬레이터에 바로 설치
./gradlew installDebug
```

Android SDK 경로는 `ANDROID_HOME` 환경 변수나 프로젝트 루트의 `local.properties`(`sdk.dir=...`)로 지정합니다.

## 프로젝트 구조

```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/fitnesstimer/
│   └── MainActivity.kt        # 타이머 로직 전체 (CountDownTimer + MediaPlayer)
└── res/
    ├── layout/activity_main.xml
    ├── raw/boxingbell.mp3     # 종료 알림음
    └── values/strings.xml
```

## 기술 스택

- Kotlin 2.0.21
- Android Gradle Plugin 8.7.3 / Gradle 8.9
- AndroidX AppCompat 1.7.0
