package com.example.imagegame;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener {
	private static final int SWIPE_THRESHOLD = 100;
	private static final int SWIPE_THRESHOLD_VELOCITY = 50;
	private final int SELECT_PICTURE = 200;
	private final int MAX_ROW = 4;
	private final int MAX_COLUMN = 4;

	private boolean imageSelected = false;
	private int moves;

	private GestureDetector gestureDetector;

	private Hashtable<String, ImageView> imagesParts;
	private Hashtable<ImageView, String> imagePositions;
	private Hashtable<ImageView, Bitmap> originalBitmaps, bitmaps;
	private ImageView selectedImagePart = null;
	private TextView moveCountsText;
	private Bitmap removedBitmap = null;
	private Button mixImageButton;

	@SuppressLint({"MissingInflatedId", "ClickableViewAccessibility"})
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Get screen dimensions
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		int squareSize = displayMetrics.widthPixels;

		// Set PartsContainer dimensions
		LinearLayout partsContainer = (LinearLayout) findViewById(R.id.partsContainer);
		ViewGroup.LayoutParams layoutParams = partsContainer.getLayoutParams();
		layoutParams.height = squareSize;
		layoutParams.width = squareSize;
		partsContainer.setLayoutParams(layoutParams);
		Log.d("DEBUG", "PartsContainer dimension set to: " + layoutParams.width + " | " + layoutParams.height);

		mixImageButton = (Button) findViewById(R.id.mixImageButton);
		moveCountsText = (TextView) findViewById(R.id.moveCountText);

		imagesParts = new Hashtable<>();
		imagePositions = new Hashtable<>();
		originalBitmaps = new Hashtable<>();
		bitmaps = new Hashtable<>();

		// Store imageParts and imagesPositions
		for (int r = 1; r < MAX_ROW + 1; r++) {
			for (int c = 1; c < MAX_COLUMN + 1; c++) {
				int imageId = getResources().getIdentifier("r" + r + "c" + c, "id", getPackageName());
				if (imageId != 0) {
					ImageView imageView = (ImageView) findViewById(imageId);

					// Set ImageView dimensions
					ViewGroup.LayoutParams imageViewParams = imageView.getLayoutParams();
					imageViewParams.height = squareSize / MAX_ROW;
					imageViewParams.width = squareSize / MAX_COLUMN;
					imageView.setLayoutParams(imageViewParams);
					Log.d("DEBUG", "ImageView(r" + r + "c" + c + ") dimension set to: " + imageViewParams.width + " | " + imageViewParams.height);

					imagesParts.put("r" + r + "c" + c, imageView);
					imagePositions.put(imageView, r + ";" + c);

					imageView.setOnTouchListener(new ImageOnTouchListener(this));
				}
			}
		}

		gestureDetector = new GestureDetector(this);
	}

	public void chooseImage(View view) {
		Intent i = new Intent();
		i.setType("image/*");
		i.setAction(Intent.ACTION_GET_CONTENT);

		startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
	}

	public void swapImageParts(ImageView firstImageView, ImageView secondImageView) {
		Bitmap firstBitmap = bitmaps.get(firstImageView);
		Bitmap secondBitmap = bitmaps.get(secondImageView);

		bitmaps.remove(firstImageView);
		bitmaps.remove(secondImageView);

		if (secondBitmap == null) {
			firstImageView.setImageBitmap(null);
			secondImageView.setImageBitmap(firstBitmap);

			bitmaps.put(secondImageView, firstBitmap);
		} else if (firstBitmap == null) {
			firstImageView.setImageBitmap(secondBitmap);
			secondImageView.setImageBitmap(null);

			bitmaps.put(firstImageView, secondBitmap);
		} else {
			firstImageView.setImageBitmap(secondBitmap);
			secondImageView.setImageBitmap(firstBitmap);

			bitmaps.put(firstImageView, secondBitmap);
			bitmaps.put(secondImageView, firstBitmap);
		}
	}
	public void mixImage(View view) {
		int emptyPartR = MAX_ROW;
		int emptyPartC = MAX_COLUMN;

		ImageView lastImageView = imagesParts.get("r" + emptyPartR + "c" + emptyPartC);
		if (lastImageView != null) {
			removedBitmap = bitmaps.get(lastImageView);
			bitmaps.remove(lastImageView);
			lastImageView.setImageBitmap(null);
		}

		Random random = new Random();
		int numberOfSwaps = 500;// + random.nextInt(1000);
		int nextR, nextC;

		for (int i = 0; i < numberOfSwaps; i++) {
			nextR = 0;
			nextC = 0;

			// Choose vertical or horizontal move
			if (random.nextInt(2) > 0) {
				// Choose right or left
				if (random.nextInt(2) > 0) {
					nextC = 1;
				} else {
					nextC = -1;
				}
			} else {
				// Choose up or down
				if (random.nextInt(2) > 0) {
					nextR = 1;
				} else {
					nextR = -1;
				}
			}

			int otherR = emptyPartR + nextR;
			int otherC = emptyPartC + nextC;

			if ((otherR > 0 && otherR <= MAX_ROW) && (otherC > 0 && otherC <= MAX_COLUMN)){
				ImageView emptyImageView = imagesParts.get("r" + emptyPartR + "c" + emptyPartC);
				ImageView otherImageView = imagesParts.get("r" + otherR + "c" + otherC);

				Log.d("DEBUG", "Moving r" + emptyPartR + "c" + emptyPartC + " -> r" + otherR + "c" + otherC);

				swapImageParts(otherImageView, emptyImageView);
				emptyPartR = otherR;
				emptyPartC = otherC;
			}
		}

		moves = 0;
		moveCountsText.setText("Moves: 0");
		mixImageButton.setVisibility(View.INVISIBLE);
	}

	public Bitmap imageToSquare(Uri imageUri) {
		InputStream inputStream;
		try {
			inputStream = getContentResolver().openInputStream(imageUri);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

		int shorterDimension, x = 0, y = 0;
		if (bitmap.getHeight() > bitmap.getWidth()) {
			shorterDimension = bitmap.getWidth();
			y = (bitmap.getHeight() - bitmap.getWidth()) / 2;
		} else {
			shorterDimension = bitmap.getHeight();
			x = (bitmap.getWidth() - bitmap.getHeight()) / 2;
		}

		// Create square image
		return Bitmap.createBitmap(bitmap, x, y, shorterDimension, shorterDimension);
	}

	public void createPuzzle(Bitmap bitmapImage) {
		int squareSize = bitmapImage.getWidth() / MAX_ROW;

		for (int r = 0; r < MAX_ROW; r++) {
			for (int c = 0; c < MAX_COLUMN; c++) {
				Bitmap croppedBitmap = Bitmap.createBitmap(bitmapImage, squareSize * c, squareSize * r, squareSize, squareSize);
				ImageView imagePart = imagesParts.get("r" + (r + 1) + "c" + (c + 1));

				if (imagePart != null) {
					imagePart.setImageBitmap(croppedBitmap);
					bitmaps.put(imagePart, croppedBitmap);
					originalBitmaps.put(imagePart, croppedBitmap);
				}
			}
		}
	}

	private boolean checkForCompletion() {
		ImageView imagePart;

		for (int r = 1; r < MAX_ROW + 1; r++) {
			for (int c = 1; c < MAX_COLUMN + 1; c++) {
				Log.d("DEBUG", "Checking r" + r + "c" + c);
				if (r != MAX_ROW || c != MAX_COLUMN) {
					imagePart = imagesParts.get("r" + r + "c" + c);
					if (bitmaps.get(imagePart) != originalBitmaps.get(imagePart))
						return false;
				}
			}
		}

		return true;
	}

	private void endGame() {
		ImageView lastImagePart = imagesParts.get("r" + MAX_ROW + "c" + MAX_COLUMN);
		if (lastImagePart == null) {
			return;
		}

		bitmaps.put(lastImagePart, removedBitmap);
		lastImagePart.setImageBitmap(removedBitmap);

		mixImageButton.setVisibility(View.VISIBLE);
	}

	// swipeMove: 1=Right -1=Left 2=Up -2=Down
	public void swipeHandler(int swipeMove) {
		// Check if there is a selectedImagePart and the selectedImagePart is not the emptyImagePart
		if (selectedImagePart == null || bitmaps.get(selectedImagePart) == null) {
			return;
		}

		String[] imagePartPosition = imagePositions.get(selectedImagePart).split(";");
		int selectedR = Integer.parseInt(imagePartPosition[0]);
		int selectedC = Integer.parseInt(imagePartPosition[1]);

		int otherR = selectedR;
		int otherC = selectedC;
//        switch (swipeMove) {
//            case 1:
//                Log.d("DEBUG", "Swipe right");
//                if (selectedC < MAX_COLUMN)
//                    otherC++;
//                break;
//            case -1:
//                Log.d("DEBUG", "Swipe left");
//                if (selectedC > 0)
//                    otherC--;
//                break;
//            case 2:
//                Log.d("DEBUG", "Swipe up");
//                if (selectedR > 0)
//                    otherR--;
//                break;
//            case -2:
//                Log.d("DEBUG", "Swipe down");
//                if (selectedR < MAX_ROW)
//                    otherR++;
//                break;
//        }

		if ((swipeMove % 2) == 0) {
			otherR = Math.max(0, Math.min(MAX_ROW, otherR + (-swipeMove / 2)));
		} else {
			otherC = Math.max(0, Math.min(MAX_COLUMN, otherC + swipeMove));
		}

		if (otherR != selectedR || otherC != selectedC) {
			ImageView otherImagePart = imagesParts.get("r" + otherR + "c" + otherC);
			if (otherImagePart != null && bitmaps.get(otherImagePart) == null) {
				swapImageParts(selectedImagePart, otherImagePart);
				moves++;
				moveCountsText.setText("Moves: " + moves);

				if (selectedR == MAX_ROW && selectedC == MAX_COLUMN) {
					Log.d("DEBUG", "Moved last part");
					if (checkForCompletion()) {
						Log.d("DEBUG", "Puzzle solved");
						endGame();
					}
				}
			}
		}

		clearSelectedImagePart();
	}

	public void setSelectedImagePart(ImageView selectedImagePart) {
		this.selectedImagePart = selectedImagePart;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		gestureDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// Process selected image
		if (resultCode == RESULT_OK && requestCode == SELECT_PICTURE) {
			Uri selectedImageUri = null;

			if (data != null) {
				selectedImageUri = data.getData();
			}

			if (selectedImageUri != null) {
				Log.d("DEBUG", "Image Selected");
				Bitmap squaredBitmap = imageToSquare(selectedImageUri);

				imageSelected = true;
				moves = 0;
				moveCountsText.setText("Moves: 0");
				mixImageButton.setVisibility(View.VISIBLE);
				createPuzzle(squaredBitmap);
			}
		}
	}

	@Override
	public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
		if (!imageSelected || e1 == null) {
			return false;
		}

		float diffX = e2.getX() - e1.getX();
		float diffY = e2.getY() - e1.getY();

		if (Math.abs(diffX) > Math.abs(diffY)) {
			if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				if (diffX > 0) {
					swipeHandler(1);
				} else {
					swipeHandler(-1);
				}
			}
		} else {
			if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
				if (diffY > 0) {
					swipeHandler(-2);
				} else {
					swipeHandler(2);
				}
			}
		}

		return false;
	}

	public void clearSelectedImagePart() {
		this.selectedImagePart = null;
	}

	@Override
	public boolean onDown(@NonNull MotionEvent e) { return false; }
	@Override
	public void onShowPress(@NonNull MotionEvent e) { }
	@Override
	public boolean onSingleTapUp(@NonNull MotionEvent e) { return false; }
	@Override
	public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) { return false; }
	@Override
	public void onLongPress(@NonNull MotionEvent e) { }
}