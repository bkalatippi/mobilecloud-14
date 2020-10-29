package org.magnum.dataup;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {
	private static final AtomicLong currentId = new AtomicLong(0L);
	private static List<Video> list = new CopyOnWriteArrayList<>();
	private static Map<Long, MultipartFile> videodata = new HashMap<>();

	@RequestMapping(value="/video", method=RequestMethod.GET)
	public @ResponseBody List<Video> getVideoList(){ 
		
		return list;
	}
	
	@RequestMapping(value = "/video", method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video video, HttpServletRequest request){ 		
		checkAndSetId(video);
		
		if(video.getDataUrl() == null) {
			video.setDataUrl(getDataUrl(video.getId()));
		}
		
		list.add(video);
		
		return video;	
	}
	
	@RequestMapping(value = "/video/{id}/data", method=RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(@PathVariable("id") Long id, @RequestParam("data") MultipartFile data, HttpServletResponse response) throws IOException{
		Video video = new Video();
		video.setId(id);
		
		boolean exists = false;
		for(Video v: list) {
			if(v.getId() == id.longValue()) {
				exists = true;
				break;
			}
		}
		
		response.setHeader("Content-Type", "application/json");
		
		if(!exists) {
			response.sendError(HttpStatus.NOT_FOUND.value());
		}else {
				VideoFileManager.get().saveVideoData(video, data.getInputStream());
				videodata.put(id, data);
				response.setStatus(HttpStatus.OK.value());
				
		}
		
		return new VideoStatus(VideoStatus.VideoState.READY);
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.GET)
   	public void getData(@PathVariable(VideoSvcApi.ID_PARAMETER) Long id, HttpServletResponse response) throws IOException {
		Video video = new Video();
		video.setId(id);
		
		boolean exists = false;
		for(Video v: list) {
			if(v.getId() == id.longValue()) {
				exists = true;
				break;
			}
		}
		
		//response.setHeader("Content-Type", "application/json");
  		if (!exists) {
 			response.setStatus(HttpStatus.NOT_FOUND.value());
 			//return response;
 		} else {
 			try {
 				VideoFileManager.get().copyVideoData(video, response.getOutputStream());
				//response.setStatus(HttpStatus.OK.value());
				response.flushBuffer();
			} catch (IOException e) {
				response.sendError(HttpStatus.NOT_FOUND.value());
				e.printStackTrace();
			}
 			//return response;
 		}
  	}
	
	private void checkAndSetId(Video video) {
		if(video.getId() == 0L){
			video.setId(currentId.incrementAndGet());
		}
	}
	
	private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

 	private String getUrlBaseForLocalServer() {
	   HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
	   String base =  "http://"+request.getServerName() + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
	   
	   return base;
	}
	
}
