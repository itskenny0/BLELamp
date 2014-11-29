package com.imagic.lamp.kevin;import android.app.Activity;import android.app.ProgressDialog;import android.bluetooth.BluetoothDevice;import android.content.Context;import android.content.Intent;import android.graphics.Bitmap;import android.graphics.Color;import android.graphics.drawable.BitmapDrawable;import android.graphics.drawable.Drawable;import android.os.Bundle;import android.util.Log;import android.view.Menu;import android.view.MenuItem;import android.widget.Button;import android.widget.LinearLayout;import android.widget.Toast;import com.imagic.lamp.kevin.ble.BLEDevice.RFStarBLEBroadcastReceiver;import com.imagic.lamp.kevin.ble.ColorTools;import com.imagic.lamp.kevin.ble.ImageRGBView;import com.imagic.lamp.kevin.ble.ImageRGBView.ImageRGBDelegate;import com.imagic.lamp.kevin.ble.LampImageView2;import com.imagic.lamp.kevin.ble.LightView;import com.imagic.lamp.kevin.ble.LightView.LightRGBDelegate;import com.imagic.lamp.kevin.ble.RFImagicBLEService;import com.imagic.lamp.kevin.ble.RFImagicManage;import com.imagic.lamp.kevin.ble.RFLampDevice;import com.imagic.lamp.kevin.ble.Tools;/* * 色盘 *  * @author kevin *  *         功能：  *         	  1）调色 *            2）调亮度 *            3) 设置闹钟 */public class LampControllerActivity extends Activity implements ImageRGBDelegate, RFStarBLEBroadcastReceiver {	/**	 * 当前颜色	 */	private int currentColor = 0;	private ImageRGBView rgbView = null; // 色盘	private LightView liangduView = null; // 调亮度	private LinearLayout layoutBg = null;	private LinearLayout rgbLayout = null;	private LampImageView2 baiquanView = null;	private Button onOffBtn = null; // 开关	private Button clockBtn = null; // 时钟	private int lightValue = 0; // 亮度值	RFLampDevice lampDevice = null;	BluetoothDevice bleDevice = null;	private void initView() {		this.layoutBg = (LinearLayout) this.findViewById(R.id.controllerLayoutBg);		this.rgbLayout = (LinearLayout) this.findViewById(R.id.rgbLayout);		this.rgbLayout.setBackground(null);		this.rgbView = new ImageRGBView(this, R.drawable.rgb, this);		this.liangduView = new LightView(this, R.drawable.liangdu);		liangduView.setOnLevelLightDelegate(new LightRGBDelegate() {			@Override			// 为真时亮度增加，为false时，亮度减少			public void lightColor(LightView view, boolean boo, int color) {				// TODO Auto-generated method stub				if (lightValue >= 15 && lightValue <= 99) { // 控制					if (boo) {						lightValue++;					} else {						lightValue--;					}					if (lightValue == 14) {						lightValue = 15;					}					int tmp = ColorTools.restoreColor(lightValue, color);					view.setLevelLight(lightValue + "%");					baiquanView.setColor(tmp);					setonOffBgColor(tmp);					lampDevice.sendLampColor(tmp, lightValue);					Log.d(RFImagicManage.RFSTAR, "66666666  light value  " + countLight(color) + "%" + "boo : " + boo);				} else if (lightValue >= 100) {					lightValue = 99;				}			}		});		this.rgbLayout.addView(this.rgbView);		this.baiquanView = (LampImageView2) this.findViewById(R.id.baiquanView);	}	/**	 * 计算亮度值	 * 	 * @param color	 */	private int countLight(int color) {		int colorArray[] = new int[3];		colorArray[0] = Color.red(color);		colorArray[1] = Color.green(color);		colorArray[2] = Color.blue(color);		int hsl[] = new int[3];		ColorTools.RGB2HSL(colorArray[0], colorArray[1], colorArray[2], hsl);		return 100 * hsl[2] / 255;	}	@Override	protected void onCreate(Bundle savedInstanceState) {		// TODO Auto-generated method stub		super.onCreate(savedInstanceState);		bleDevice = this.getIntent().getParcelableExtra(RFImagicManage.RFSTAR);		setContentView(R.layout.lamp_controller_activity);		this.initView();		this.setTitle(bleDevice.getAddress());		this.lampDevice = RFImagicManage.getInstance().getLampDeviceByBLEDevice(bleDevice);		this.lampDevice.setBLEBroadcastDelegate(this);		dialog = new ProgressDialog(this);		dialog.setMessage("正在加载服务");		dialog.setCanceledOnTouchOutside(false);		dialog.setCancelable(false);		dialog.show();	}	@Override	protected void onPause() {		// TODO Auto-generated method stub		super.onPause();		System.out.println("LampControllerActivity.onPause()");	}	@Override	protected void onDestroy() {		super.onDestroy();		dialog.dismiss();		System.out.println("LampControllerActivity.onDestroy()");	}	/*	 * 设置onOffBtn后面的背景色	 * 	 * @param color	 */	@SuppressWarnings({ "deprecation", "static-access" })	private void setonOffBgColor(int color) {		Drawable drawable = getResources().getDrawable(R.drawable.dingshi_n);		BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;		Bitmap bitmap = bitmapDrawable.getBitmap();		BitmapDrawable bbb = new BitmapDrawable(Tools.toRoundCorner(bitmap, color));		lampDevice.currentColor = color;	}	@Override	public void imageColor(int color) {		// TODO Auto-generated method stub		this.baiquanView.setColor(color);		this.setonOffBgColor(color);		this.sendColorData(color);		lightValue = countLight(color);		liangduView.setLevelLight(lightValue + "%");		this.currentColor = color;	}	/**	 * 发送color值	 * 	 * @param color	 */	private void sendColorData(int color) {		lampDevice.sendLampColor(color, 0);	}	private static int RGB = 0;	private static int LIANGDU = 1;	private int isRgbOrLiangdu = RGB;	@Override	public boolean onCreateOptionsMenu(Menu menu) {		// Inflate the menu; this adds items to the action bar if it is present.		getMenuInflater().inflate(R.menu.lamp_controller, menu);		if (isRgbOrLiangdu == RGB) {			menu.findItem(R.id.menu_liangdu).setVisible(false);			menu.findItem(R.id.menu_rgb).setVisible(true);		} else {			menu.findItem(R.id.menu_liangdu).setVisible(true);			menu.findItem(R.id.menu_rgb).setVisible(false);		}		return true;	}	@Override	public boolean onOptionsItemSelected(MenuItem item) {		rgbLayout.removeAllViews();		switch (item.getItemId()) {		case R.id.menu_rgb:			liangduView.setColor(currentColor);			rgbLayout.addView(liangduView);			isRgbOrLiangdu = LIANGDU;			break;		case R.id.menu_liangdu:			rgbLayout.addView(rgbView);			isRgbOrLiangdu = RGB;			break;		}		runOnUiThread(new Runnable() {			@Override			public void run() {				// TODO Auto-generated method stub				invalidateOptionsMenu();			}		});		return true;	}	private ProgressDialog dialog = null;	@Override	public void onReceive(Context context, Intent intent, String macData, String uuid) {		// TODO Auto-generated method stub		String action = intent.getAction();		if (RFImagicBLEService.ACTION_GATT_CONNECTED.equals(action)) {			Log.d(RFImagicManage.RFSTAR, "111111111 连接完成");			this.setTitle(macData + "已连接");			dialog.hide();			Toast.makeText(this, "已连接", Toast.LENGTH_SHORT).show();		} else if (RFImagicBLEService.ACTION_GATT_DISCONNECTED.equals(action)) {			Log.d(RFImagicManage.RFSTAR, "111111111 连接断开");			this.setTitle(macData + "已断开");			dialog.hide();			Toast.makeText(this, "已断开", Toast.LENGTH_SHORT).show();		} else if (RFImagicBLEService.ACTION_DATA_AVAILABLE.equals(action)) {		} else if (RFImagicBLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {			dialog.hide();		}	}}