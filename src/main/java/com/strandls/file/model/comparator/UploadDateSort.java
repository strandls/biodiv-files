package com.strandls.file.model.comparator;

import java.util.Comparator;

import com.strandls.file.model.MyUpload;

public class UploadDateSort implements Comparator<MyUpload> {

	@Override
	public int compare(MyUpload obj1, MyUpload obj2) {
		if (obj1 == null || obj2 == null) {
			return 1;
		}
		System.out.println(obj1 + " " + obj2);
		return obj1.getDateUploaded().compareTo(obj2.getDateUploaded());
	}
	
}