package com.imagic.lamp.kevin.ble;import android.bluetooth.BluetoothDevice;import android.bluetooth.BluetoothGattCharacteristic;import android.bluetooth.BluetoothGattService;import android.content.Context;import android.graphics.Color;import android.util.Log;/* * 蓝牙灯属性 *      功能： *        1) 保存属性 *        2) 读取属性 *        4) 调亮度 *        5) 开关 *        6) 获取services * @author Kevin.wu * */public class RFLampDevice extends BLEDevice {	public static final String LED_ServiceUUID = "ffb0";	/**	 * ChangePlay, ChangeStatePoint, ChangeMode(0-> Default LED SET(设定指定值) | 1->	 * LED STOP(停止变化) | 2-> RGB FLASH(闪变) | 3-> RGB STROBE(频闪) | 4-> RGB	 * FADE(褪色) | 5-> RGB SMOOTH(顺变)), ChangePeriod	 */	public static final String LED_RW_ChangeModeCharateristicUUID = "ffb1";	// hsl值	public static final String LED_RW_DataCharateristicUUID = "ffb2";	// Year_L, Year_H, Month, Day, Hour, Minute, Second	public static final String LED_RW_SystemTimeCharateristicUUID = "ffb3";	/**	 * Timer_On_Enabel, Timer_On_Hour, Timer_On_Minute, Timer_On_Fade_In,	 * Timer_Off_Enabel, Timer_Off_Hour, Timer_Off_Minute, Timer_Off_Fade_Out	 */	public static final String LED_RW_TimeOnOffCharateristicUUID = "ffb4";	/**	 * Total_Time_Day_L, Total_Time_Day_H, Total_Time_Hour, Total_Time_Minute,	 * Current_Time_Hour_L, Current_Time_Hour_H, Current_Time_Minute	 */	public static final String LED_RN_WorkTimeCharateristicUUID = "ffb5";	// GroupID, LampID, LampType, LockLastState	public static final String LED_RW_GroupIDCharateristicUUID = "ffb6";	public static final String LED_RW_LampNameCharateristicUUID = "ffb7";	/**	 * Disconnect 0-> Default | 1-> Disconnect	 */	public static final String LED_W_DisconnectCharateristicUUID = "ffb8";	public static final String LED_RW_r_ArrayCharateristicUUID = "ffb9";	public static final String LED_RW_g_ArrayCharateristicUUID = "ffba";	public static final String LED_RW_b_ArratCharateristicUUID = "ffbb";	public static final String AESCheckServiceUUID = "fc60";	public static final String AESCheckCharateristicUUID = "fc64";	public static final String TESTCharateristicShouUUID = "ffe4"; // 接收数据	public static final String TESTCharateristicFaUUID = "ffe9";	// 获取的特征值	public BluetoothGattCharacteristic LED_RW_ChangeModeCharateristic;	public BluetoothGattCharacteristic LED_RW_DataCharateristic;	public BluetoothGattCharacteristic LED_RW_SystemTimeCharateristic;	public BluetoothGattCharacteristic LED_RW_TimeOnOffCharateristic;	public BluetoothGattCharacteristic LED_RN_WorkTimeCharateristic;	public BluetoothGattCharacteristic LED_RW_GroupIDCharateristic;	public BluetoothGattCharacteristic LED_RW_LampNameCharateristic;	public BluetoothGattCharacteristic LED_W_DisconnectCharateristic;	public BluetoothGattCharacteristic LED_RW_r_ArrayCharateristic;	public BluetoothGattCharacteristic LED_RW_g_ArrayCharateristic;	public BluetoothGattCharacteristic LED_RW_b_ArratCharateristic;	// led灯类型	public static final int OLD_RGBW_Type = 3;	public static final int Switch_type = 0xf0;	public static final int Brightness_Type = 0xe0;	public static final int Hue_Type = 0xd0;	public static final int CCT_Type = 0xc0, i = 0xef;	public boolean onOffState = true; // 灯的状态	// 加密使能	public static boolean ENABLE_ENCODE = true;// YES | NO	// 定义CheckOut类型	public static final int CheckRequest = 0x99;	public static final int CheckSucceed = 0x24;	public static final int CheckFailed = 0x47;	public static final int LED_CONTROLLER_FADE_IN_OUT_LENGTH = 3;	public static final byte WN_AES64_CHECK_LENGTH = 9; // CheckType,														// RandomNumber[2],	// MacAddress[6]	/**	 * 灯的当前颜色	 */	public int currentColor = Color.WHITE;	public RFLampDevice(Context context, BluetoothDevice device) {		// TODO Auto-generated constructor stub		super(context, device);		bleDeviceType = OLD_RGBW_Type;	}	public RFLampDevice(Context context, BluetoothDevice device,int type) {		// TODO Auto-generated constructor stub		super(context, device);		bleDeviceType = type;	}	/**	 * 设置灯	 * 	 * @param lampType	 */	public void setLampType(int lampType) {		this.bleDeviceType = lampType;	}	/**	 * 验证是否是本公司的的移动设备	 * 	 * @param characteristic	 */	private void checkUUID(BluetoothGattCharacteristic characteristic) {		Log.d(RFImagicApp.KTag, "6666  :" + characteristic.getUuid());		int ran = (int) (Math.random() * 100);		Log.d(RFImagicApp.KTag, "6666   随机数 ： ran " + ran);		Log.d(RFImagicApp.KTag, "6666   随机数 ： hi  " + ((ran >> 8) & 0xFF));		Log.d(RFImagicApp.KTag, "6666   随机数 ： lo  " + (ran & 0xFF));		byte hiRan = (byte) ((ran >> 8) & 0xFF);		byte lowRan = (byte) (ran & 0xFF);		if (ENABLE_ENCODE) // 加密使能		{			byte[] encodeByte = new byte[WN_AES64_CHECK_LENGTH + 1];			byte[] decodeByte = { (byte) CheckRequest, hiRan, lowRan, 0x12,					0x23, 0x34, 0x45, 0x56, 0x67 };			this.encodeArray(WN_AES64_CHECK_LENGTH, encodeByte, decodeByte);			byte[] encodeData = new byte[WN_AES64_CHECK_LENGTH + 1];			encodeData = encodeByte;			Log.d(RFImagicApp.KTag, "6666  write:   " + Tools.byte2Hex(encodeData));			characteristic.setValue(encodeData);			writeValue(characteristic);		} else {			byte decodeByte[] = { (byte) CheckRequest, hiRan, lowRan, 0x12,					0x23, 0x34, 0x45, 0x56, 0x67 };			characteristic.setValue(decodeByte);			writeValue(characteristic);		}		readValue(characteristic);		Log.d(RFImagicApp.KTag,				"6666  read：  " + Tools.byte2Hex(characteristic.getValue()));	}	/*	 * 初始化特征值 从服务中扫描特征值	 */	@Override	protected void discoverCharacteristicsFromService() {		if (bleService.getSupportedGattServices(this.device) != null) {			Log.w(RFImagicApp.KTag, "wwwwww  discover  service size : "					+ bleService.getSupportedGattServices(this.device).size());		}		for (BluetoothGattService service : bleService				.getSupportedGattServices(this.device)) { // 迭代服务			for (BluetoothGattCharacteristic characteristic : service					.getCharacteristics()) { // 迭代特征值				if (characteristic.getUuid().toString()						.contains(AESCheckCharateristicUUID)) { // 验证					this.checkUUID(characteristic);				} else if (characteristic.getUuid().toString()						.contains(LED_RW_ChangeModeCharateristicUUID)) { // 更改模式					Log.d(RFImagicApp.KTag, "6666  changeModeCharateristic");				} else if (characteristic.getUuid().toString()						.contains(LED_RW_DataCharateristicUUID)) {					Log.d(RFImagicApp.KTag, "6666  dataCharateristic");					LED_RW_DataCharateristic = characteristic;					readValue(LED_RW_DataCharateristic);				} else if (characteristic.getUuid().toString()						.contains(LED_RW_SystemTimeCharateristicUUID)) {					Log.d(RFImagicApp.KTag, "6666  systemTimeCharateristic");					LED_RW_SystemTimeCharateristic = characteristic;					characteristic.setValue(getSystemTime());					writeValue(characteristic);				} else if (characteristic.getUuid().toString()						.contains(LED_RW_TimeOnOffCharateristicUUID)) {					LED_RW_TimeOnOffCharateristic = characteristic;					readValue(LED_RW_TimeOnOffCharateristic);				} else if (characteristic.getUuid().toString()						.contains(LED_RN_WorkTimeCharateristicUUID)) {					// 获取蓝牙设备工作的时间长					Log.d(RFImagicApp.KTag, "6666   workTimeCharateristic");					LED_RN_WorkTimeCharateristic = characteristic;					// setCharacteristicNotification(LED_RN_WorkTimeCharateristic,					// true);					// readValue(LED_RN_WorkTimeCharateristic);				} else if (characteristic.getUuid().toString()						.contains(LED_RW_GroupIDCharateristicUUID)) { // 分组					LED_RW_GroupIDCharateristic = characteristic;					readValue(LED_RW_GroupIDCharateristic);					Log.d(RFImagicApp.KTag,							"22222 uuid value  "									+ Tools.byte2Hex(LED_RW_GroupIDCharateristic											.getValue()));				} else if (characteristic.getUuid().toString()						.contains(LED_RW_LampNameCharateristicUUID)) {					LED_RW_LampNameCharateristic = characteristic;					readValue(LED_RW_LampNameCharateristic);				} else if (characteristic.getUuid().toString()						.contains(LED_W_DisconnectCharateristicUUID)) { // 完成连接					LED_W_DisconnectCharateristic = characteristic;				} else if (characteristic.getUuid().toString()						.contains(LED_RW_r_ArrayCharateristicUUID)) {					LED_RW_r_ArrayCharateristic = characteristic;					// writeValue(characteristic);				} else if (characteristic.getUuid().toString()						.contains(LED_RW_g_ArrayCharateristicUUID)) {					LED_RW_g_ArrayCharateristic = characteristic;					// writeValue(characteristic);				} else if (characteristic.getUuid().toString()						.contains(LED_RW_b_ArratCharateristicUUID)) {					LED_RW_b_ArratCharateristic = characteristic;					// writeValue(characteristic);				} else if (characteristic.getUuid().toString()						.contains(TESTCharateristicFaUUID)) {					Log.d(RFImagicApp.KTag, "6666  :" + characteristic.getUuid());					characteristic.setValue("1234567".getBytes());					// writeValue(characteristic);				}			}		}		if (LED_RW_GroupIDCharateristic != null) {			sendGroupID();			Log.d(RFImagicApp.KTag, "22222 uuid start");		}	}	/**	 * 获取蓝牙设备的系统时间	 */	public void sendLampTime() {		if (LED_RW_SystemTimeCharateristic != null) {			readValue(LED_RW_SystemTimeCharateristic);		}	}	/**	 * 发送开关时间	 * 	 * @param onOffTime	 */	public void sendOnOffTime(byte[] onOffTime) {		if (LED_RW_TimeOnOffCharateristic != null) {			Log.d(RFImagicApp.KTag,					"7777777  timeOnOff : " + Tools.byte2Hex(onOffTime));			LED_RW_TimeOnOffCharateristic.setValue(onOffTime);			writeValue(LED_RW_TimeOnOffCharateristic);		}	}	/**	 * 发送 color数据到设备	 * 	 * @param color	 */	public void sendLampColor(int color, int brightness) {		int red, green, blue;		red = Color.red(color);		blue = Color.blue(color);		green = Color.green(color);		Log.d(RFImagicApp.KTag, "22222 rgb    red:" + red + "  green:" + green				+ " blue:" + blue);		byte[] tmpColor = null;		if (bleDeviceType == Hue_Type) {			tmpColor = new byte[4];			tmpColor[0] = (byte) Hue_Type;			tmpColor[1] = (byte) red;			tmpColor[2] = (byte) green;			tmpColor[3] = (byte) blue;			Log.d(RFImagicApp.KTag, "22222 sendlamp hue_type");		} else if (bleDeviceType == OLD_RGBW_Type) {			tmpColor = new byte[3];			tmpColor[0] = (byte) red;			tmpColor[1] = (byte) green;			tmpColor[2] = (byte) blue;			Log.d(RFImagicApp.KTag, "22222 sendlamp old_RGB");		} else if (((int) bleDeviceType & 0xf0) == Brightness_Type) { // 色温灯			Log.d(RFImagicApp.KTag, "99999999999  brightness_type");			tmpColor = new byte[2];			tmpColor[0] = (byte) color;			tmpColor[1] = (byte) Color.alpha(color);		} else if (((int) bleDeviceType & 0xf0) == Switch_type) { // 开关灯			Log.d(RFImagicApp.KTag, "99999999999  switch_Type");		} else if (((int) bleDeviceType & 0xf0) == CCT_Type) {			Log.d(RFImagicApp.KTag, "99999999999  cct_type");		}		if (LED_RW_DataCharateristic != null) {			LED_RW_DataCharateristic.setValue(tmpColor);		}		this.writeValue(LED_RW_DataCharateristic);		// this.readValue(LED_RW_DataCharateristic);	}	/**	 * 获取 当前的颜色	 */	public void sendReadCurrentColor() {		if (LED_RW_DataCharateristic != null) {			this.readValue(LED_RW_DataCharateristic);		} else {			Log.d(RFImagicApp.KTag, "33333  LED_RW_DataCharateristic is null ");		}	}	/**	 * 调亮度	 * 	 * @param level	 */	public void sendBrightness(byte level) {		byte brightnessByte[] = new byte[2];		brightnessByte[0] = (byte) Brightness_Type;		brightnessByte[1] = level;		this.writeValue(LED_RW_DataCharateristic);	}	/**	 * 灯开关	 * 	 * @param boo	 *            true为开，false 为关	 */	public void onOFFLamp(boolean boo) {		if (bleDeviceType == Switch_type) {		}		if (bleDeviceType == Hue_Type || bleDeviceType == OLD_RGBW_Type) {			if (boo) {				this.sendLampColor(currentColor, 0);				onOffState = true;			} else {				this.sendLampColor(Color.TRANSPARENT, 0);				onOffState = false;			}		}	}	/**	 * 发送消息,灯的类别	 */	@Override	public void sendGroupID() {		readValue(LED_RW_GroupIDCharateristic);	}	/*	 * 组合特征值	 * 	 * @param uuid	 * 	 * @return	 */	String getSubUUID(String uuid) {		return "0000" + RFLampDevice.AESCheckCharateristicUUID				+ "-0000-1000-8000-00805F9B34FB";	}}