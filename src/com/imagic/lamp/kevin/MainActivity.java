package com.imagic.lamp.kevin;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.imagic.lamp.kevin.ble.BLEDevice.RFStarBLEBroadcastReceiver;
import com.imagic.lamp.kevin.ble.RFImagicBLEService;
import com.imagic.lamp.kevin.ble.RFImagicManage;
import com.imagic.lamp.kevin.ble.RFImagicManage.RFImagicManageListener;
import com.imagic.lamp.kevin.ble.RFLampDevice;
import com.imagic.lamp.util.Utils;

public class MainActivity extends Activity implements RFImagicManageListener, OnItemClickListener, RFStarBLEBroadcastReceiver, BluetoothAdapter.LeScanCallback {
	private RFImagicManage manager = null;
	private ListView list = null;
	private DeviceListAdapter listAdapter = null;
	private BluetoothAdapter bleAdapter = null;
	
	private ProgressDialog dialog = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 检察系统是否包含蓝牙低功耗的jar包
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		BluetoothManager blmanager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		bleAdapter = blmanager.getAdapter();
		// 检察手机硬件是否支持蓝牙低功耗
		if (bleAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		list = (ListView) this.findViewById(R.id.listView1);
		manager = RFImagicManage.getInstance();
		manager.setBluetoothAdapter(bleAdapter);
		manager.setRFstarBLEManagerListener(this);
		listAdapter = new DeviceListAdapter(this);
		list.setAdapter(listAdapter);
		list.setOnItemClickListener(this);
		
		dialog = new ProgressDialog(this);
		dialog.setMessage("正在扫描设备");
		dialog.setCanceledOnTouchOutside(false);
		dialog.setCancelable(false);
		
		requestBluetooth();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		manager.stopScanBluetoothDevice();
	}

	@Override
	protected void onResume() {
		super.onResume();
		manager.clearBLEDevices();
		listAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		manager.unRegisterAllDevice();
		dialog.dismiss();
	}

	/**
	 * 设置权限后，返回时调用
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == Utils.REQUEST_CODE ) {
			if (resultCode == Activity.RESULT_CANCELED) {
				Toast.makeText(this, R.string.open_bluetooth, Toast.LENGTH_SHORT).show();
			} else if (resultCode == Activity.RESULT_OK) {
				requestBluetooth();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		if (!manager.getScanningState()) {
			menu.findItem(R.id.menu_stop).setVisible(false);
			menu.findItem(R.id.menu_scan).setVisible(true);
			menu.findItem(R.id.menu_refresh).setActionView(null);
		} else {
			menu.findItem(R.id.menu_stop).setVisible(true);
			menu.findItem(R.id.menu_scan).setVisible(false);
			menu.findItem(R.id.menu_refresh).setActionView(R.layout.rfimagic_progress);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_scan:
			listAdapter.notifyDataSetChanged();
			requestBluetooth();
			break;
		case R.id.menu_stop:
			manager.stopScanBluetoothDevice();
			break;
		}
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		BluetoothDevice device = this.manager.getBLEDevice(position);
		
		Intent intent = new Intent(this, LampControllerActivity.class);
		intent.putExtra(Utils.RFSTAR, device);
		startActivity(intent);
		manager.stopScanBluetoothDevice();
	}

	/**
	 * 扫描到设备
	 */
	@Override
	public void RFstarBLEManageListener(BluetoothDevice device, int rssi, byte[] scanRecord, int lampType) {
		listAdapter.notifyDataSetChanged();
		manager.addLampDevice(new RFLampDevice(this, device, lampType));
	}

	/**
	 * 开始扫描
	 */
	@Override
	public void RFstarBLEManageStartScan() {
		// TODO Auto-generated method stub
		invalidateOptionsMenu();
	}

	/**
	 * 扫描停止
	 */
	@Override
	public void RFstarBLEManageStopScan() {
		// TODO Auto-generated method stub
		manager.stopScanBluetoothDevice();
		invalidateOptionsMenu();
		dialog.hide();
	}

	@Override
	public void onReceive(Context context, Intent intent, String macData, String uuid) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		if (RFImagicBLEService.ACTION_GATT_CONNECTED.equals(action)) {
			Log.d(Utils.RFSTAR, "111111111 连接完成");
			this.setTitle(macData + "已连接");
		} else if (RFImagicBLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
			Log.d(Utils.RFSTAR, "111111111 连接断开");
			this.setTitle(macData + "已断开");
		} else if (RFImagicBLEService.ACTION_DATA_AVAILABLE.equals(action)) {

		}
	}
	
	private void requestBluetooth() {
		if (bleAdapter == null) {
			return;
		}

		if (bleAdapter.isEnabled()) {
			dialog.show();
			manager.startScanBluetoothDevice();
		} else {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, Utils.REQUEST_CODE);
		}
	}
	
	private Handler handler = new Handler();
	
	@Override
	public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
		// 添加扫描到的device，并刷新数据
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (!manager.hasBLEDevice(device)) {
					manager.addBLEDevice(device, rssi, scanRecord);
				}
			}
		});
	}

}
