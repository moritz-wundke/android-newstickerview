package com.mystictreegames.ui.newstickerview;

/**
 * Static class containing useful mathematic related methods.
 * @author Moritz 'Moss' Wundke (b.thax.dcg@gmail.com)
 *
 */
public class Utils {
	
	/** Shifter used to set an alpha byte into a argb color integer value */
	public static final int ALPHA_COLOR_SHIFTER = 24;
	
	/**
	 * Clamp a float between to given ones
	 */
	public static float clamp(float x, float a, float b) {
		return x < a ? a : (x > b ? b : x);
	}
	
	/** Combine a rgb/argb value with a linear alpha into a simple argb */
	public static int combineColor(int rgb, float alpha) {
		int iAlpha = ((alpha) >= 1.0 ? 255 : (int)((alpha)*256.0));
		// Make sure to get rid of the original colors alpha
		int argb = (rgb & 0x00FFFFFF) | ( iAlpha << ALPHA_COLOR_SHIFTER);
		return argb;
	}
}
