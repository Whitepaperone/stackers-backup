package com.ssafy.stackers.service;

import com.ssafy.stackers.exception.CustomException;
import com.ssafy.stackers.model.Video;
import com.ssafy.stackers.repository.VideoRepository;
import com.ssafy.stackers.utils.S3Uploader;
import com.ssafy.stackers.utils.error.ErrorCode;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import net.bramp.ffmpeg.builder.FFmpegBuilder.Strict;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VideoService {

    @Autowired
    private VideoRepository videoRepository;
    private final S3Uploader s3Uploader;

    @Transactional
    public void save(Video video) {
        Video v = Video.builder().videoName(video.getVideoName())
            .videoPath(video.getVideoPath()).build();
        videoRepository.save(v);
    }

    public Video findById(Long videoId){
        Video v = videoRepository.findById(videoId).orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));
        return v;
    }

    /**
     * S3 데이터베이스 파일 업로드
     */
    public Video uploadVideoToS3(MultipartFile file, String videoName, Long prevStationId, int remainDepth, String prevPath) throws IOException{
        String localPath = saveLocalFile(file);     // 로컬에 임시 저장
        localPath = encodeVideo(localPath);         // 인코딩
        localPath = cropVideo(localPath);           // 크롭

        if(prevStationId != -1 && remainDepth != 3) {   // '참여하기'일 경우 기존 영상과 합치기
            localPath = mergeVideo(prevPath, localPath);
        }

        File processedFile = new File(localPath);

        String fileName = "static/videos/" + UUID.randomUUID() + processedFile.getName();
        String videoPath = s3Uploader.putS3(processedFile, fileName);
        removeLocalFile(processedFile);
        Video video = Video.builder().videoName(videoName).videoPath(videoPath).build();
        return video;
    }

    /**
     * 데이터베이스 파일 삭제
     * @param filePath
     * @throws Exception
     */
    public void deleteVideoFromS3(String filePath) throws Exception {
        s3Uploader.deleteS3(filePath);
    }

    /**
     * 로컬 데이터베이스에 비디오 업로드
     * */
    public Video uploadVideo(MultipartFile file) throws IOException {
        String sourceVideoName = file.getOriginalFilename();
        String sourceVideoNameExtension = FilenameUtils.getExtension(sourceVideoName).toLowerCase();
        FilenameUtils.removeExtension(sourceVideoName);

        File destinationFile;
        String destinationFileName;
        String videoPath = "C:\\stackers\\videos\\";

        do {
            destinationFileName =
                RandomStringUtils.randomAlphanumeric(64) + "." + sourceVideoNameExtension;
            destinationFile = new File(videoPath + destinationFileName);
        } while (destinationFile.exists());

        destinationFile.getParentFile().mkdirs();
        file.transferTo(destinationFile);

        Video video = Video.builder().videoName(destinationFileName)
            .videoPath(videoPath).build();

        return video;
    }

    /**
     * MultipartFile -> File 변환, 로컬에 파일 저장, 경로 반환
     */
    private String saveLocalFile(MultipartFile file) {
        // 로컬에서 저장할 파일 경로 : user.dir => 현재 디렉토리 기준
        String dirPath = System.getProperty("user.dir") + "/" + file.getOriginalFilename();
        File convertFile = new File(dirPath);

        try {
            convertFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // FileOutputStream 데이터를 파일에 바이트 스트림으로 저장
        try (FileOutputStream fos = new FileOutputStream(convertFile)) {
            fos.write(file.getBytes());
            return dirPath;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 로컬에 저장된 파일 삭제
     */
    private void removeLocalFile(File targetFile) {
        if (targetFile.delete()) {
            log.info("[파일 업로드] : 파일 삭제 성공");
            return;
        }
        log.info("[파일 업로드] : 파일 삭제 실패");
    }

    /**
     * 동영상 썸네일 추출
     * 동영상 파일 경로, 썸네일 추출 경로 (프로젝트 외부) 절대 경로로 지정함
     */
    public void exportThumbnail() throws IOException {
        // 영상 파일 경로
        String videoPath = "C:\\test\\videos\\test.mp4";

        // 썸네일 추출 절대 경로
        String thumbnailPath = "C:\\test\\thumbs\\";

        // ffmpeg 설치 파일 경로 -> 환경 변수로 설정
        FFmpeg ffmpeg = new FFmpeg("ffmpeg");
        FFprobe ffprobe = new FFprobe("ffprobe");

        // 썸네일 추출 명령어
        FFmpegBuilder builder = new FFmpegBuilder()
            .setInput(videoPath)                // 영상 파일 경로
            .overrideOutputFiles(true)          // 썸네일 파일 존재할 경우 덮어쓰기
            .addOutput(thumbnailPath + "thumb.png")   // 썸네일 추출 절대 경로
            .addExtraArgs("-ss", "00:00:01")            // 썸네일 추출 시작점 ([영상 길이 / 10]으로 10개 뽑을 예정)
            .setFrames(1)
            .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg,
            ffprobe);        // FFmpeg 명령어 실행을 위한 FFmpegExecutor 객체 생성
        executor.createJob(builder).run();                                    // one-pass encodes
    }

    /**
     * 동영상 메타데이터 추출
     */
    public void getMetaData() throws IOException {
        // 영상 파일 경로
        String videoPath = "C:\\test\\videos\\test.mp4";

        // ffmpeg 설치 파일 경로 -> 환경 변수로 설정
        FFmpeg ffmpeg = new FFmpeg("ffmpeg");
        FFprobe ffprobe = new FFprobe("ffprobe");

        FFmpegProbeResult probeResult = ffprobe.probe(videoPath);

        log.info("===== Video Meta Data =====");
        log.info("duration: " + probeResult.getStreams().get(0).duration);
        log.info("codec name: " + probeResult.getStreams().get(0).codec_name);
        log.info("width: " + probeResult.getStreams().get(0).width);
        log.info("height: " + probeResult.getStreams().get(0).height);
        log.info("bit_rate: " + probeResult.getStreams().get(0).bit_rate);
        log.info("avg_frame_rate: " + probeResult.getStreams().get(0).avg_frame_rate);

    }

    /**
     * 동영상 인코딩
     */
    public String encodeVideo(String localPath) throws IOException {
        // 인코딩 파일 추출 경로
        String encodePath = localPath + "_E";

        // ffmpeg 설치 파일 경로 -> 환경 변수로 설정
        FFmpeg ffmpeg = new FFmpeg("ffmpeg");
        FFprobe ffprobe = new FFprobe("ffprobe");

        // 동영상 인코딩 명령어
        FFmpegBuilder builder = new FFmpegBuilder()
            .setInput(localPath)                                    // 영상 파일 경로
            .overrideOutputFiles(true)                              // 인코딩 파일 존재할 경우 덮어쓰기
            .addOutput(encodePath)        // 인코딩 파일 경로
            .setFormat("mp4")               // 인코딩 파일 형식
//            .setTargetSize(2130_000)        // 인코딩 목표 용량 (KB)
//            .disableSubtitle()              // 자막 없음
//            .setAudioChannels(1)            // mono audio
//            .setAudioCodec("aac")           // 오디오 코덱
//            .setAudioSampleRate(48_000)     // 48KHz : 오디오 샘플 레이트
//            .setAudioBitRate(32768)         // 32kbit/s : 오디오 비트 레이트 (오디오 품질, 높을수록 좋음)
//            .setVideoCodec("libx264")       // 비디오 코덱
//            .setVideoFrameRate(24, 1)    // 24 frames per second
            .setVideoResolution(1080, 720)
            .setStrict(Strict.EXPERIMENTAL)
            .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

        // one-pass encode
        executor.createJob(builder, p -> {
                    if(p.isEnd()) {
                        System.out.println("!!!" + encodePath + " 동영상 인코딩 성공!!!");
                    }
                }
        ).run();

        removeLocalFile(new File(encodePath));

        return encodePath;
    }

    /**
     * 동영상 크롭
     * width:405 height:720
     */
    public String cropVideo(String localPath) throws IOException {
        // 크롭한 영상 추출 경로
        String cropPath = localPath + "_C";

        // ffmpeg 설치 파일 경로 -> 환경 변수로 설정
        FFmpeg ffmpeg = new FFmpeg("ffmpeg");
        FFprobe ffprobe = new FFprobe("ffprobe");

        FFmpegBuilder builder = new FFmpegBuilder()
                .addInput(localPath)
                .addOutput(cropPath)
                .addExtraArgs("-vf", "crop=270:480")
//            .addExtraArgs("-vf", "crop=405:720")
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

        // one-pass encode
        executor.createJob(builder, p -> {
                    if(p.isEnd()) {
                        System.out.println("!!!" + cropPath + " 동영상 크롭 성공!!!");
                    }
                }
        ).run();

        removeLocalFile(new File(cropPath));
        
        return cropPath;
    }

    /**
     * 동영상 합치기
     * ffmpeg 명령어 그대로 사용함 -> 배포 시 수정 필요
     */
    public String mergeVideo(String prevPath, String localPath) throws IOException {
        // 합친 영상 추출 경로
        String mergePath = localPath + "_M";

        // ffmpeg 설치 파일 경로 -> 환경 변수로 설정
        FFmpeg ffmpeg = new FFmpeg("ffmpeg");
        FFprobe ffprobe = new FFprobe("ffprobe");

        FFmpegBuilder builder = new FFmpegBuilder()
            .overrideOutputFiles(true)
            .addInput(prevPath)
            .addInput(localPath)
            .addOutput(mergePath)
            .addExtraArgs("-preset", "ultrafast")
            .addExtraArgs("-filter_complex",
//                "[0:v]setpts=PTS-STARTPTS, pad=iw*2+5:ih[bg]; [1:v]setpts=PTS-STARTPTS[fg]; [bg][fg]overlay=w+5")
                "[0:v][1:v]hstack=inputs=2[v]; [0:a][1:a]amerge[a]")
            .addExtraArgs("-map", "[v]")
            .addExtraArgs("-map", "[a]")
            .addExtraArgs("-ac", "2")
            .addExtraArgs("-vsync", "0")
            .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

        // one-pass encode
        executor.createJob(builder, p -> {
                if(p.isEnd()) {
                    System.out.println("!!!" + mergePath + " 동영상 합치기 성공!!!");
                }
            }
        ).run();

        removeLocalFile(new File(mergePath));

        return mergePath;
    }
}
