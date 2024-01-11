package com.example.imagegame;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class ImageOnTouchListener implements View.OnTouchListener {
	MainActivity mainActivity;

	public ImageOnTouchListener(MainActivity mainActivity) {
		this.mainActivity = mainActivity;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mainActivity.setSelectedImagePart((ImageView) v);
				break;
			case MotionEvent.ACTION_UP:
				mainActivity.clearSelectedImagePart();
				break;
		}

		return false;
	}
}
