package com.google.devrel.training.conference.domain;

import com.google.appengine.api.memcache.MemcacheService; 
import com.google.appengine.api.memcache.MemcacheServiceFactory;
// Get the Memcache Service MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();


public class Announcement {
	private String message;
	
	private Announcement(){}
	
	public Announcement(String message){
		this.message = message;
	}
	
	public String getMessage()
	{
		return message;
	}
}
