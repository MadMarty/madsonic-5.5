/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package github.madmarty.madsonic.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.RemoteControlClient;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.support.v4.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import github.madmarty.madsonic.R;
import github.madmarty.madsonic.domain.MusicDirectory;
import github.madmarty.madsonic.service.MusicService;
import github.madmarty.madsonic.service.MusicServiceFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Asynchronous loading of images, with caching.
 * <p/>
 * There should normally be only one instance of this class.
 *
 * @author Sindre Mehus
 */
@TargetApi(14)
public class ImageLoader implements Runnable {

    private static final Logger LOG = new Logger(ImageLoader.class);
    private static final int CONCURRENCY = 5;

	private Handler mHandler = new Handler();
	private Context context;
	
    private LruCache<String, Bitmap> cache;
    private final BlockingQueue<Task> queue;
    
    private final int imageSizeDefault;
	private final int imageSizeMedium;
    private final int imageSizeLarge;

//    private final int imageSizeSmall  = 128;
//    private final int imageSizeMedium = 192;
//    private final int imageSizeLarge  = 256;
//    private final int imageSizeXLarge = 512;
    
    private Drawable largeUnknownImage;

    public ImageLoader(Context context) {
    	
		this.context = context;
		
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		final int cacheSize = maxMemory / 4;
		
		cache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
			}
			
			@Override
			protected void entryRemoved(boolean evicted, String key, Bitmap oldBitmap, Bitmap newBitmap) {
				if(evicted) {
					try {
						oldBitmap.recycle();
                	} catch(Exception e) {
                		// Do nothing, just means that the drawable is a flat image
                	}
				}
			}
		};
		
        queue = new LinkedBlockingQueue<Task>(500);

        // Determine the density-dependent image sizes.
        imageSizeDefault = (int) Math.round((context.getResources().getDrawable(R.drawable.unknown_album).getIntrinsicHeight())); 
        LOG.info( "imageSizeDefault: " + imageSizeDefault );

        imageSizeMedium = 180; // (int) Math.round((context.getResources().getDrawable(R.drawable.unknown_album_medium).getIntrinsicHeight()));;
        LOG.info( "imageSizeMedium: " + imageSizeMedium );
        
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        imageSizeLarge = (int) Math.round(Math.min(metrics.widthPixels, metrics.heightPixels) * 0.6);
        LOG.info( "imageSizeLarge: " + imageSizeLarge );
        
//        imageSizeDefault = Util.getCoverSize(context);
        
        for (int i = 0; i < CONCURRENCY; i++) {
            new Thread(this, "ImageLoader").start();
        }

        createLargeUnknownImage(context);
    }

    private void createLargeUnknownImage(Context context) {
    	
        BitmapDrawable drawable = (BitmapDrawable) context.getResources().getDrawable(R.drawable.unknown_album);
        Bitmap bitmap = Bitmap.createScaledBitmap(drawable.getBitmap(), imageSizeLarge, imageSizeLarge, true);
        bitmap = createReflection(bitmap);
        largeUnknownImage = Util.createDrawableFromBitmap(context, bitmap);
    }

    // WRAP for reflection
    public void loadImage(View view, MusicDirectory.Entry entry, boolean large, boolean crossfade) {
    	loadImage(view, entry, large, crossfade, large);
        }    
    
    public void loadImage(View view, MusicDirectory.Entry entry, boolean large, boolean crossfade, boolean reflection) {
        if (entry == null || entry.getCoverArt() == null) {
            setUnknownImage(view, large);
            return;
        }

        int size = large ? imageSizeLarge : imageSizeDefault;
        
//        Bitmap bitmap = cache.get(getKey(entry.getCoverArt(), size));
//        if (bitmap != null) {
//			final Drawable drawable = Util.createDrawableFromBitmap(this.context, bitmap);
//            setImage(view, drawable, large);
//            return;

        Bitmap bitmap = cache.get(getKey(entry.getCoverArt(), size));
        if (bitmap != null) {
            // Create a clone since the images can be modified by the caller.
        	Drawable drawable = Util.createDrawableFromBitmap(this.context, bitmap);
            Drawable clone = drawable.getConstantState().newDrawable();
            setImage(view, clone, large);
            return;
        }

        if (!large) {
            setUnknownImage(view, large);
        }
        queue.offer(new Task(view.getContext(), entry, size, imageSizeLarge, large ? true : false, new ViewTaskHandler(view, crossfade)));
	}

    public void loadImage(Context context, RemoteControlClient remoteControl, MusicDirectory.Entry entry) {
        if (entry == null || entry.getCoverArt() == null) {
            setUnknownImage(remoteControl);
            return;
        }
        
        Bitmap bitmap = cache.get(getKey(entry.getCoverArt(), imageSizeLarge));
        if (bitmap != null) {
			Drawable drawable = Util.createDrawableFromBitmap(this.context, bitmap);
            setImage(remoteControl, drawable);
            return;
        }

        setUnknownImage(remoteControl);
        queue.offer(new Task(context, entry, imageSizeLarge, imageSizeLarge, false, new RemoteControlClientTaskHandler(remoteControl)));
    }

    private String getKey(String coverArtId, int size) {
        return coverArtId + size;
    }

    @SuppressWarnings("deprecation")
	private void setImage(View view, Drawable drawable, boolean crossfade) {
        if (view instanceof TextView) {
            // Cross-fading is not implemented for TextView since it's not in use.  It would be easy to add it, though.
            TextView textView = (TextView) view;
            textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        } else if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            if (crossfade) {

                Drawable existingDrawable = imageView.getDrawable();
                if (existingDrawable == null) {
					Bitmap emptyImage;
                    if(drawable.getIntrinsicWidth() > 0 && drawable.getIntrinsicHeight() > 0) {
						emptyImage = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    } else {
                    	emptyImage = Bitmap.createBitmap(imageSizeDefault, imageSizeDefault, Bitmap.Config.ARGB_8888);
                    }
                    existingDrawable = new BitmapDrawable(emptyImage);
                } else {
                	// Try to get rid of old transitions
                	try {
                		TransitionDrawable tmp = (TransitionDrawable) existingDrawable;
                		int layers = tmp.getNumberOfLayers();
                		existingDrawable = tmp.getDrawable(layers - 1);
                	} catch(Exception e) {
                		// Do nothing, just means that the drawable is a flat image
                	}
                }

                Drawable[] layers = new Drawable[]{existingDrawable, drawable};

                TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
                imageView.setImageDrawable(transitionDrawable);
                transitionDrawable.startTransition(250);
            } else {
                imageView.setImageDrawable(drawable);
            }
        }
    }
    
	private void setImage(RemoteControlClient remoteControl, Drawable drawable) {
		if(remoteControl != null && drawable != null) {
			Bitmap origBitmap = ((BitmapDrawable)drawable).getBitmap();
			remoteControl.editMetadata(false)
			.putBitmap(
					RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK,
    			origBitmap.copy(origBitmap.getConfig(), true))
			.apply();
		}
    }

    private void setUnknownImage(View view, boolean large) {
        if (large) {
            setImage(view, largeUnknownImage, true );
        } else {
            if (view instanceof TextView) {
                ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(R.drawable.unknown_album, 0, 0, 0);
            } else if (view instanceof ImageView) {
                ((ImageView) view).setImageResource(R.drawable.unknown_album);
            }
        }
    }
    
    private void setUnknownImage(RemoteControlClient remoteControl) {
        setImage(remoteControl, largeUnknownImage);
    }

    public void clear() {
        queue.clear();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Task task = queue.take();
                task.execute();
            } catch (Throwable x) {
                LOG.error("Unexpected exception in ImageLoader.", x);
            }
        }
    }

    private Bitmap createReflection(Bitmap originalImage) {

    //	int reflectionH = 80;
    	
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // Height of reflection
        int reflectionHeight = height / 2;
        
        // The gap we want between the reflection and the original image
        final int reflectionGap = 4;

        // Create a new bitmap with same width but taller to fit reflection
        Bitmap bitmapWithReflection = Bitmap.createBitmap(width, (height + reflectionHeight), Bitmap.Config.ARGB_8888);
        
        //// ----
        
    	Bitmap reflection = Bitmap.createBitmap(width,reflectionHeight, Bitmap.Config.ARGB_8888);
    	Bitmap blurryBitmap = Bitmap.createBitmap(originalImage, 0, height - reflectionHeight, height, reflectionHeight);

    	// cheap and easy scaling algorithm; down-scale it, then
    	// upscale it. The filtering during the scale operations
    	// will blur the resulting image
    	blurryBitmap = Bitmap.createScaledBitmap(
    	Bitmap.createScaledBitmap(
    	blurryBitmap,blurryBitmap.getWidth() / 2,
    	blurryBitmap.getHeight() / 2, true),
    	blurryBitmap.getWidth(), blurryBitmap.getHeight(), true);
    	
    	// This shadier will hold a cropped, inverted,
    	// blurry version of the original image
    	BitmapShader bitmapShader = new BitmapShader(blurryBitmap, TileMode.CLAMP, TileMode.CLAMP);
    	Matrix invertMatrix = new Matrix();
    	invertMatrix.setScale(1f, -1f);
    	invertMatrix.preTranslate(0, -reflectionHeight);
    	bitmapShader.setLocalMatrix(invertMatrix);

    	// This shader holds an alpha gradient
    	Shader alphaGradient = new LinearGradient(0, 0, 0, reflectionHeight, 0x80ffffff, 0x00000000, TileMode.CLAMP);

    	// This shader combines the previous two, resulting in a
    	// blurred, fading reflection
    	ComposeShader compositor = new ComposeShader(bitmapShader, alphaGradient, PorterDuff.Mode.DST_IN);

    	Paint reflectionPaint = new Paint();
    	reflectionPaint.setShader(compositor);

    	// Draw the reflection into the bitmap that we will return
    	Canvas canvas = new Canvas(reflection);
    	canvas.drawRect(0, 0, reflection.getWidth(), reflection.getHeight(), reflectionPaint);

    	/// -----
    	
        // Create a new Canvas with the bitmap that's big enough for
        // the image plus gap plus reflection
        Canvas finalcanvas = new Canvas(bitmapWithReflection);

        // Draw in the original image
        finalcanvas.drawBitmap(originalImage, 0, 0, null);

        // Draw in the gap
        Paint defaultPaint = new Paint();
        
        // transparent gap
        defaultPaint.setColor(0);
        
        finalcanvas.drawRect(0, height, width, height + reflectionGap, defaultPaint);

        // Draw in the reflection
        finalcanvas.drawBitmap(reflection, 0, height + reflectionGap, null);
    	
    	return bitmapWithReflection;
    }
        
	private class Task {
    	private final Context mContext;
        private final MusicDirectory.Entry mEntry;
        private final int mSize;
        private final int mSaveSize;
        private final boolean mReflection;
        private ImageLoaderTaskHandler mTaskHandler;

        public Task(Context context, MusicDirectory.Entry entry, int size, int saveSize, boolean reflection, ImageLoaderTaskHandler taskHandler) {
        	mContext = context;
            mEntry = entry;
            mSize = size;
            mReflection = reflection;
            mSaveSize = saveSize;
            mTaskHandler = taskHandler;
        }

        public void execute() {
            try {
				loadImage();
			} catch(OutOfMemoryError e) {
				LOG.warn( "Ran out of memory trying to load image, try cleanup and retry");
				cache.evictAll();
				System.gc();
			}
        }
		public void loadImage() {
			try {
                MusicService musicService = MusicServiceFactory.getMusicService(mContext);
                Bitmap bitmap = musicService.getCoverArt(mContext, mEntry, mSize, mSaveSize, null);
                String key = getKey(mEntry.getCoverArt(), mSize);

                if (mReflection) {
                    bitmap = createReflection(bitmap);
                }
                
                if (mSize != imageSizeLarge) {
                    cache.put(key, bitmap);
                    // Make sure key is the most recently "used"
                    cache.get(key);
                }
                
				final Drawable drawable = Util.createDrawableFromBitmap(mContext, bitmap);
                mTaskHandler.setDrawable(drawable);
                mHandler.post(mTaskHandler);
            } catch (Throwable x) {
                LOG.error( "Failed to download album art.", x);
            }
		}
    }
	
	private abstract class ImageLoaderTaskHandler implements Runnable {
		
		protected Drawable mDrawable;
		
		public void setDrawable(Drawable drawable) {
			mDrawable = drawable;
		}
		
	}
	
	private class ViewTaskHandler extends ImageLoaderTaskHandler {

		protected boolean mCrossfade;
		private View mView;
		
		public ViewTaskHandler(View view, boolean crossfade) {
			mCrossfade = crossfade;
			mView = view;
		}
		
		@Override
		public void run() {
			setImage(mView, mDrawable, mCrossfade);
		}
	}
	
	private class RemoteControlClientTaskHandler extends ImageLoaderTaskHandler {
		
		private RemoteControlClient mRemoteControl;
		
		public RemoteControlClientTaskHandler(RemoteControlClient remoteControl) {
			mRemoteControl = remoteControl;
		}
		
		@Override
		public void run() {
			setImage(mRemoteControl, mDrawable);
		}
	}
}
