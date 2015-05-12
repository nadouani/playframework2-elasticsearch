package com.github.eduardofcbg.plugin.es;

import java.util.List;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;

public class Result<T extends Index> {

	public Long number;
	public List<T> contents;
	public T content;
	
	public IndexResponse responseFromIndexing;
	public DeleteResponse responseFromDeleting;
	
	public Result(Long number, List<T> contents, T content, IndexResponse responseFromIndexing, DeleteResponse responseFromDeleting) {
		if (contents != null) number = (long) contents.size();
		if (content != null) number = (long) 1;
		this.number = number;
		this.contents = contents;
		this.content = content;
		this.responseFromIndexing = responseFromIndexing;
		this.responseFromDeleting = responseFromDeleting;
	}
	
	
	
}
