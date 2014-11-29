package com.imagic.lamp.kevin;

import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

public class MainActivity extends Activity implements RFImagicManageListener, OnItemClickListener, RFStarBLEBroadcastReceiver {
	private RFImagicManage manager = null;
	private ListView list = null;
	private BAdapter bleAdapter = null;
	private ArrayList<BluetoothDevice> arraySource = new ArrayList<BluetoothDevice>();

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
		BluetoothAdapter adapter = blmanager.getAdapter();
		// 检察手机硬件是否支持蓝牙低功耗
		if (adapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		list = (ListView) this.findViewById(R.id.listView1);
		manager = RFImagicManage.getInstance();
		manager.setBluetoothAdapter(adapter);
		manager.setRFstarBLEManagerListener(this);
		bleAdapter = new BAdapter(this, arraySource);
		list.setAdapter(bleAdapter);
		list.setOnItemClickListener(this);
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
		arraySource.clear();
		bleAdapter.notifyDataSetChanged();
		manager.isEdnabled(this);
		manager.startScanBluetoothDevice();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		manager.unRegisterAllDevice();
	}

	/**
	 * 设置权限后，返回时调用
	 * 
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// User chose not to enable Bluetooth.
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == RFImagicManage.REQUEST_CODE && resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
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
			if (this.arraySource != null) {
				this.arraySource.clear();
				bleAdapter.notifyDataSetChanged();
			}
			manager.startScanBluetoothDevice();
			break;
		case R.id.menu_stop:
			manager.stopScanBluetoothDevice();
			break;
		}
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub

		BluetoothDevice device = this.arraySource.get(position);

		Intent intent = new Intent(this, LampControllerActivity.class);
		intent.putExtra(RFImagicManage.RFSTAR, device);
		startActivity(intent);
		manager.stopScanBluetoothDevice();
	}

	/*
	 * 扫描到设备时，调用 (non-Javadoc)
	 * 
	 * @see com.rfstar.antilost.kevin.ble.RFstarManage.RFstarManageListener#
	 * RFstarBLEManageListener(android.bluetooth.BluetoothDevice, int,
	 * byte[],int)
	 */
	@Override
	public void RFstarBLEManageListener(BluetoothDevice device, int rssi, byte[] scanRecord, int lampType) {
		// TODO Auto-generated method stub
		arraySource.add(device);
		bleAdapter.notifyDataSetChanged();

		manager.addLampDevice(new RFLampDevice(this, device, lampType));
	}

	/*
	 * 开始扫描防丢器设备 (non-Javadoc)
	 * 
	 * @see com.rfstar.antilost.kevin.ble.RFstarManage.RFstarManageListener#
	 * RFstarBLEManageStartScan()
	 */
	@Override
	public void RFstarBLEManageStartScan() {
		// TODO Auto-generated method stub
		invalidateOptionsMenu();
	}

	/*
	 * 扫描停止后调用 (non-Javadoc) 如：listview停止刷新
	 * 
	 * @see com.rfstar.antilost.kevin.ble.RFstarManage.RFstarManageListener#
	 * RFstarBLEManageStopScan()
	 */
	@Override
	public void RFstarBLEManageStopScan() {
		// TODO Auto-generated method stub
		manager.stopScanBluetoothDevice();
		invalidateOptionsMenu();
	}

	@Override
	public void onReceive(Context context, Intent intent, String macData, String uuid) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		if (RFImagicBLEService.ACTION_GATT_CONNECTED.equals(action)) {
			Log.d(RFImagicManage.RFSTAR, "111111111 连接完成");
			this.setTitle(macData + "已连接");
		} else if (RFImagicBLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
			Log.d(RFImagicManage.RFSTAR, "111111111 连接断开");
			this.setTitle(macData + "已断开");
		} else if (RFImagicBLEService.ACTION_DATA_AVAILABLE.equals(action)) {

		}
	}

}
