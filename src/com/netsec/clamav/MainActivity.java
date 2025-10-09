package com.netsec.clamav;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.netsec.clamav.databinding.ActivityMainBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    final int ACTIVITY_CHOOSE_FILE = 1;
    static int uploadResponseCode = 0;
    Uri selectedFileUri = null;

    private ActivityMainBinding binding;
    private List<ScanRecord> history = new ArrayList<>();
    private ScanHistoryAdapter adapter;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "ClamScanPrefs";
    private static final String PREFS_HISTORY_KEY = "scan_history_json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set the Event Listeners for all the UI controls
        binding.fileSelectorButton.setOnClickListener(this);
        binding.uploadButton.setOnClickListener(this);
        binding.downloadButton.setOnClickListener(this);
        binding.scanNowButton.setOnClickListener(this);

        // Setup history RecyclerView and preferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        adapter = new ScanHistoryAdapter(history, new ScanHistoryAdapter.OnItemClickListener() {
            @Override
            public void onViewReport(int position, ScanRecord record) {
                if (record != null && record.reportPath != null) {
                    try {
                        File f = new File(record.reportPath);
                        if (f.exists()) {
                            Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                                    MainActivity.this, getPackageName() + ".fileprovider", f);
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(contentUri, "text/plain");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        } else {
                            Toast.makeText(MainActivity.this, "Report file not found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Unable to open report", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "No report available", Toast.LENGTH_SHORT).show();
                }
            }
        });
        binding.scanHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.scanHistoryRecyclerView.setAdapter(adapter);
        binding.scanHistoryRecyclerView.setHasFixedSize(true);
        loadHistoryFromPrefs();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // File selector
            case R.id.fileSelectorButton:
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, ACTIVITY_CHOOSE_FILE);
                break;

            case R.id.uploadButton:
                // Upload to Server using SAF Uri
                if (selectedFileUri != null) {
                    binding.progressIndicator.setVisibility(View.VISIBLE);
                    new Thread(new Runnable() {
                        public void run() {
                            uploadResponseCode = UploadFileToServer.uploadFile(MainActivity.this, selectedFileUri);
                            // hide progress and update UI
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    binding.progressIndicator.setVisibility(View.GONE);
                                    if (uploadResponseCode == 200) {
                                        binding.scanCompleteTextView.setText("File Scan Complete!!");
                                        uploadResponseCode = 0;
                                        binding.downloadButton.setVisibility(View.VISIBLE);
                                    } else {
                                        binding.scanCompleteTextView.setText("Oops!! Error uploading file.");
                                        uploadResponseCode = 0;
                                    }
                                }
                            });
                        }
                    }).start();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Please select a file to upload!!", Toast.LENGTH_LONG)
                            .show();
                }
                break;

            case R.id.scanNowButton:
                // Trigger scan: reuse upload flow but record history when complete
                if (selectedFileUri != null) {
                    binding.progressIndicator.setVisibility(View.VISIBLE);
                    new Thread(new Runnable() {
                        public void run() {
                            int code = UploadFileToServer.uploadFile(MainActivity.this, selectedFileUri);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    binding.progressIndicator.setVisibility(View.GONE);
                                    if (code == 200) {
                                        binding.scanCompleteTextView.setText("File Scan Complete!!");
                                        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                                        String fname = (selectedFileUri.getLastPathSegment() != null) ? selectedFileUri.getLastPathSegment() : selectedFileUri.toString();
                                        ScanRecord rec = new ScanRecord(ts, fname, "OK", null);
                                        history.add(0, rec);
                                        adapter.notifyItemInserted(0);
                                        saveHistoryToPrefs();
                                    } else {
                                        binding.scanCompleteTextView.setText("Oops!! Error uploading file.");
                                    }
                                }
                            });
                        }
                    }).start();
                } else {
                    Toast.makeText(MainActivity.this, "Please select a file to scan!!", Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.downloadButton:
                // Download from server activity — after successful download, write local path to most recent history entry and persist
                binding.progressIndicator.setVisibility(View.VISIBLE);
                new Thread(new Runnable() {
                    // new thread to start the activity
                    public void run() {
                        try {
                            File downloadedFile = DownLoadFileFromServer.downloadFile(MainActivity.this);

                            // Update the most recent history entry with the local path if present
                            if (!history.isEmpty()) {
                                ScanRecord latest = history.get(0);
                                latest.reportPath = downloadedFile.getAbsolutePath();
                                // persist
                                saveHistoryToPrefs();
                            }

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    binding.progressIndicator.setVisibility(View.GONE);
                                }
                            });

                            // open the downloaded file for viewing
                            Intent intent = new Intent();
                            intent.setAction(android.content.Intent.ACTION_VIEW);
                            try {
                                Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                                        MainActivity.this, getPackageName() + ".fileprovider", downloadedFile);
                                intent.setDataAndType(contentUri, "text/plain");
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(intent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            reset();
                        } catch (final Exception e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    binding.progressIndicator.setVisibility(View.GONE);
                                    Toast.makeText(MainActivity.this, "Error downloading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }).start();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ACTIVITY_CHOOSE_FILE: {
                if (resultCode == RESULT_OK && data != null) {
                    Uri uri = data.getData();
                    selectedFileUri = uri;
                    binding.selectedFileTextView.setText("Selected File: " + uri.toString());
                }
            }
        }
    }

    // Note: when using ACTION_OPEN_DOCUMENT we work with the Uri directly
    // and stream via ContentResolver instead of resolving a direct file path.

    // reset all the controls to null
    public void reset() {
        runOnUiThread(new Runnable() {
            public void run() {
                binding.downloadButton.setVisibility(View.GONE);
                selectedFileUri = null;
                binding.selectedFileTextView.setText(null);
                binding.scanCompleteTextView.setText(null);
            }
        });
    }

    // Persist history to SharedPreferences as a simple JSON array
    private void saveHistoryToPrefs() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (ScanRecord r : history) {
                if (!first) sb.append(",");
                first = false;
                sb.append('{');
                sb.append("\"timestamp\":\"").append(escapeJson(r.timestamp)).append("\"");
                sb.append(",\"filename\":\"").append(escapeJson(r.filename)).append("\"");
                sb.append(",\"result\":\"").append(escapeJson(r.result)).append("\"");
                sb.append(",\"reportPath\":\"").append(escapeJson(r.reportPath)).append("\"");
                sb.append('}');
            }
            sb.append("]");
            prefs.edit().putString(PREFS_HISTORY_KEY, sb.toString()).apply();
        } catch (Exception e) {
            Log.e("MainActivity", "Error saving history", e);
        }
    }

    private void loadHistoryFromPrefs() {
        try {
            String json = prefs.getString(PREFS_HISTORY_KEY, "[]");
            history.clear();
            if (json == null || json.length() < 3) return;
            String trimmed = json.trim();
            if (!trimmed.startsWith("[")) return;
            String body = trimmed.substring(1, trimmed.length() - 1).trim();
            if (body.length() == 0) return;
            String[] items = body.split("\\},\\{");
            for (int i = 0; i < items.length; i++) {
                String s = items[i];
                if (!s.startsWith("{")) s = "{" + s;
                if (!s.endsWith("}")) s = s + "}";
                String ts = extractJsonField(s, "timestamp");
                String fn = extractJsonField(s, "filename");
                String res = extractJsonField(s, "result");
                String rp = extractJsonField(s, "reportPath");
                history.add(new ScanRecord(ts, fn, res, rp));
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Log.e("MainActivity", "Error loading history", e);
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String extractJsonField(String obj, String key) {
        try {
            String q = "\"" + key + "\"\\s*:\\s*\\\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(q + "(.*?)\\\"");
            java.util.regex.Matcher m = p.matcher(obj);
            if (m.find()) return m.group(1).replace("\\\\", "\\").replace("\\\"", "\"");
        } catch (Exception e) { }
        return null;
    }

}
package com.netsec.clamav;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.SharedPreferences;
import android.util.Log;

import android.content.Intent;
import android.net.Uri;
package com.netsec.clamav;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.netsec.clamav.databinding.ActivityMainBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

	final int ACTIVITY_CHOOSE_FILE = 1;
	static int uploadResponseCode = 0;
	Uri selectedFileUri = null;

	private ActivityMainBinding binding;
	private List<ScanRecord> history = new ArrayList<>();
	private ScanHistoryAdapter adapter;
	private SharedPreferences prefs;
	private static final String PREFS_NAME = "ClamScanPrefs";
	private static final String PREFS_HISTORY_KEY = "scan_history_json";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		// Set the Event Listeners for all the UI controls
		binding.fileSelectorButton.setOnClickListener(this);
		binding.uploadButton.setOnClickListener(this);
		binding.downloadButton.setOnClickListener(this);
		binding.scanNowButton.setOnClickListener(this);

		// Setup history RecyclerView and preferences
		prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		adapter = new ScanHistoryAdapter(history, new ScanHistoryAdapter.OnItemClickListener() {
			@Override
			public void onViewReport(int position, ScanRecord record) {
				if (record != null && record.reportPath != null) {
					try {
						File f = new File(record.reportPath);
						if (f.exists()) {
							Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
									MainActivity.this, getPackageName() + ".fileprovider", f);
							Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.setDataAndType(contentUri, "text/plain");
							intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
							startActivity(intent);
						} else {
							Toast.makeText(MainActivity.this, "Report file not found", Toast.LENGTH_SHORT).show();
						}
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(MainActivity.this, "Unable to open report", Toast.LENGTH_SHORT).show();
					}
				} else {
					Toast.makeText(MainActivity.this, "No report available", Toast.LENGTH_SHORT).show();
				}
			}
		});
		binding.scanHistoryRecyclerView.setAdapter(adapter);
		binding.scanHistoryRecyclerView.setHasFixedSize(true);
		loadHistoryFromPrefs();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			// File selector
			case R.id.fileSelectorButton:
				Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.setType("*/*");
				startActivityForResult(intent, ACTIVITY_CHOOSE_FILE);
				break;

			case R.id.uploadButton:
				// Upload to Server using SAF Uri
				if (selectedFileUri != null) {
					binding.progressIndicator.setVisibility(View.VISIBLE);
					new Thread(new Runnable() {
						public void run() {
							uploadResponseCode = UploadFileToServer.uploadFile(MainActivity.this, selectedFileUri);
							// hide progress and update UI
							runOnUiThread(new Runnable() {
								public void run() {
									binding.progressIndicator.setVisibility(View.GONE);
									if (uploadResponseCode == 200) {
										binding.scanCompleteTextView.setText("File Scan Complete!!");
										uploadResponseCode = 0;
										binding.downloadButton.setVisibility(View.VISIBLE);
									} else {
										binding.scanCompleteTextView.setText("Oops!! Error uploading file.");
										uploadResponseCode = 0;
									}
								}
							});
						}
					}).start();
				} else {
					Toast.makeText(MainActivity.this,
							"Please select a file to upload!!", Toast.LENGTH_LONG)
							.show();
				}
				break;

			case R.id.scanNowButton:
				// Trigger scan: reuse upload flow but record history when complete
				if (selectedFileUri != null) {
					binding.progressIndicator.setVisibility(View.VISIBLE);
					new Thread(new Runnable() {
						public void run() {
							int code = UploadFileToServer.uploadFile(MainActivity.this, selectedFileUri);
							runOnUiThread(new Runnable() {
								public void run() {
									binding.progressIndicator.setVisibility(View.GONE);
									if (code == 200) {
										binding.scanCompleteTextView.setText("File Scan Complete!!");
										String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
										String fname = (selectedFileUri.getLastPathSegment() != null) ? selectedFileUri.getLastPathSegment() : selectedFileUri.toString();
										ScanRecord rec = new ScanRecord(ts, fname, "OK", null);
										history.add(0, rec);
										adapter.notifyItemInserted(0);
										saveHistoryToPrefs();
									} else {
										binding.scanCompleteTextView.setText("Oops!! Error uploading file.");
									}
								}
							});
						}
					}).start();
				} else {
					Toast.makeText(MainActivity.this, "Please select a file to scan!!", Toast.LENGTH_LONG).show();
				}
				break;

			case R.id.downloadButton:
				// Download form server activity
				binding.progressIndicator.setVisibility(View.VISIBLE);
				new Thread(new Runnable() {
					//new thread to start the activity
					public void run() {
						try {
							File downloadedFile = DownLoadFileFromServer.downloadFile(MainActivity.this);
							runOnUiThread(new Runnable() {
								public void run() {
									binding.progressIndicator.setVisibility(View.GONE);
								}
							});
							Intent intent = new Intent();
							intent.setAction(android.content.Intent.ACTION_VIEW);
							try {
								Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
										MainActivity.this, getPackageName() + ".fileprovider", downloadedFile);
								intent.setDataAndType(contentUri, "text/plain");
								intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
								startActivity(intent);
							} catch (Exception e) {
								e.printStackTrace();
							}
							reset();
						} catch (Exception e) {
							e.printStackTrace();
							runOnUiThread(new Runnable() {
								public void run() {
									binding.progressIndicator.setVisibility(View.GONE);
									Toast.makeText(MainActivity.this, "Error downloading file", Toast.LENGTH_LONG).show();
								}
							});
						}
					}
				}).start();
				break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case ACTIVITY_CHOOSE_FILE: {
				if (resultCode == RESULT_OK && data != null) {
					Uri uri = data.getData();
					selectedFileUri = uri;
					binding.selectedFileTextView.setText("Selected File: " + uri.toString());
				}
			}
		}
	}

	// Note: when using ACTION_OPEN_DOCUMENT we work with the Uri directly
	// and stream via ContentResolver instead of resolving a direct file path.

	// reset all the controls to null
	public void reset() {
		runOnUiThread(new Runnable() {
			public void run() {
				binding.downloadButton.setVisibility(View.GONE);
				selectedFileUri = null;
				binding.selectedFileTextView.setText(null);
				binding.scanCompleteTextView.setText(null);
			}
		});
	}

	// Persist history to SharedPreferences as a simple JSON array
	private void saveHistoryToPrefs() {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			boolean first = true;
			for (ScanRecord r : history) {
				if (!first) sb.append(",");
				first = false;
				sb.append('{');
				sb.append("\"timestamp\":\"").append(escapeJson(r.timestamp)).append("\"");
				sb.append(",\"filename\":\"").append(escapeJson(r.filename)).append("\"");
				sb.append(",\"result\":\"").append(escapeJson(r.result)).append("\"");
				sb.append(",\"reportPath\":\"").append(escapeJson(r.reportPath)).append("\"");
				sb.append('}');
			}
			sb.append("]");
			prefs.edit().putString(PREFS_HISTORY_KEY, sb.toString()).apply();
		} catch (Exception e) {
			Log.e("MainActivity", "Error saving history", e);
		}
	}

	private void loadHistoryFromPrefs() {
		try {
			String json = prefs.getString(PREFS_HISTORY_KEY, "[]");
			history.clear();
			if (json == null || json.length() < 3) return;
			String trimmed = json.trim();
			if (!trimmed.startsWith("[")) return;
			String body = trimmed.substring(1, trimmed.length() - 1).trim();
			if (body.length() == 0) return;
			String[] items = body.split("\\},\\{");
			for (int i = 0; i < items.length; i++) {
				String s = items[i];
				if (!s.startsWith("{")) s = "{" + s;
				if (!s.endsWith("}")) s = s + "}";
				String ts = extractJsonField(s, "timestamp");
				String fn = extractJsonField(s, "filename");
				String res = extractJsonField(s, "result");
				String rp = extractJsonField(s, "reportPath");
				history.add(new ScanRecord(ts, fn, res, rp));
			}
			adapter.notifyDataSetChanged();
		} catch (Exception e) {
			Log.e("MainActivity", "Error loading history", e);
		}
	}

	private static String escapeJson(String s) {
		if (s == null) return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}

	private static String extractJsonField(String obj, String key) {
		try {
			String q = "\"" + key + "\"\\s*:\\s*\\\"";
			java.util.regex.Pattern p = java.util.regex.Pattern.compile(q + "(.*?)\\\"");
			java.util.regex.Matcher m = p.matcher(obj);
			if (m.find()) return m.group(1).replace("\\\\", "\\").replace("\\\"", "\"");
		} catch (Exception e) { }
		return null;
	}

}
									binding.scanCompleteTextView.setText("Oops!! Error uploading file.");
								}
							}
						});
					}
				}).start();
			} else {
				Toast.makeText(MainActivity.this, "Please select a file to scan!!", Toast.LENGTH_LONG).show();
			}
			break;
				binding.downloadButton.setVisibility(View.GONE);
				selectedFileUri = null;
				binding.selectedFileTextView.setText(null);
				binding.scanCompleteTextView.setText(null);
			}
		});
	}
}
