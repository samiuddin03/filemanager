// MainActivity.java
package com.example.filemanager;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1;
    private static final int REQUEST_MANAGE_ALL_FILES = 2;

    private ListView listView;
    private TextView currentPathTextView;
    private FileAdapter fileAdapter;
    private File currentDirectory;
    private List<File> fileList;
    private File selectedFile; // For operations like copy, move
    private boolean isMoving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = findViewById(R.id.file_list_view);
        currentPathTextView = findViewById(R.id.current_path);

        registerForContextMenu(listView);

        // Check and request permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, REQUEST_MANAGE_ALL_FILES);
            } else {
                initFileExplorer();
            }
        } else {
            checkStoragePermission();
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File selectedFile = fileList.get(position);

                if (selectedFile.isDirectory()) {
                    navigateToDirectory(selectedFile);
                } else {
                    openFile(selectedFile);
                }
            }
        });
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSIONS);
        } else {
            initFileExplorer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initFileExplorer();
            } else {
                Toast.makeText(this, "Permission denied. Cannot access files.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_ALL_FILES) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    initFileExplorer();
                } else {
                    Toast.makeText(this, "Permission denied. Cannot access files.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void initFileExplorer() {
        currentDirectory = Environment.getExternalStorageDirectory();
        loadFileList();
    }

    private void loadFileList() {
        fileList = new ArrayList<>();

        currentPathTextView.setText(currentDirectory.getAbsolutePath());

        if (currentDirectory.exists() && currentDirectory.canRead()) {
            File[] files = currentDirectory.listFiles();

            if (files != null) {
                fileList.clear();

                // Add parent directory if not in root
                if (!currentDirectory.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                    fileList.add(currentDirectory.getParentFile());
                }

                // Add all files and directories
                fileList.addAll(Arrays.asList(files));

                // Sort: folders first, then files alphabetically
                Collections.sort(fileList, new Comparator<File>() {
                    @Override
                    public int compare(File file1, File file2) {
                        if (file1.isDirectory() && !file2.isDirectory()) {
                            return -1;
                        } else if (!file1.isDirectory() && file2.isDirectory()) {
                            return 1;
                        } else {
                            return file1.getName().compareToIgnoreCase(file2.getName());
                        }
                    }
                });

                fileAdapter = new FileAdapter(this, fileList);
                listView.setAdapter(fileAdapter);
            } else {
                Toast.makeText(this, "Unable to access this directory", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Permission denied to access this directory", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToDirectory(File directory) {
        if (directory.isDirectory() && directory.canRead()) {
            currentDirectory = directory;
            loadFileList();
        } else {
            Toast.makeText(this, "Cannot open this directory", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFile(File file) {
        // Get MIME type
        String type = getFileMimeType(file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            intent.setDataAndType(contentUri, type);
        } else {
            intent.setDataAndType(Uri.fromFile(file), type);
        }

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileMimeType(File file) {
        String name = file.getName();
        String extension = name.substring(name.lastIndexOf(".") + 1).toLowerCase();

        switch (extension) {
            case "txt": return "text/plain";
            case "pdf": return "application/pdf";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "mp3": return "audio/mp3";
            case "mp4": return "video/mp4";
            case "doc":
            case "docx": return "application/msword";
            case "xls":
            case "xlsx": return "application/vnd.ms-excel";
            case "zip": return "application/zip";
            default: return "*/*";
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        File selectedFile = fileList.get(info.position);

        // Skip options for parent directory
        if (selectedFile.equals(currentDirectory.getParentFile())) {
            return;
        }

        menu.setHeaderTitle(selectedFile.getName());

        if (selectedFile.isDirectory()) {
            getMenuInflater().inflate(R.menu.folder_context_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.file_context_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final File selectedFile = fileList.get(info.position);

        switch (item.getItemId()) {
            case R.id.action_rename:
                renameFile(selectedFile);
                return true;

            case R.id.action_delete:
                deleteFile(selectedFile);
                return true;

            case R.id.action_copy:
                this.selectedFile = selectedFile;
                isMoving = false;
                Toast.makeText(this, "Navigate to destination and select Paste", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.action_move:
                this.selectedFile = selectedFile;
                isMoving = true;
                Toast.makeText(this, "Navigate to destination and select Paste", Toast.LENGTH_SHORT).show();
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new_folder:
                createNewFolder();
                return true;

            case R.id.action_new_file:
                createNewFile();
                return true;

            case R.id.action_paste:
                if (selectedFile != null) {
                    if (isMoving) {
                        moveFile(selectedFile, currentDirectory);
                    } else {
                        copyFile(selectedFile, currentDirectory);
                    }
                    selectedFile = null;
                } else {
                    Toast.makeText(this, "No file selected to paste", Toast.LENGTH_SHORT).show();
                }
                return true;

            case R.id.action_refresh:
                loadFileList();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void createNewFolder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Folder");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String folderName = input.getText().toString().trim();
                if (!folderName.isEmpty()) {
                    File newFolder = new File(currentDirectory, folderName);
                    if (!newFolder.exists()) {
                        if (newFolder.mkdir()) {
                            Toast.makeText(MainActivity.this, "Folder created", Toast.LENGTH_SHORT).show();
                            loadFileList();
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to create folder", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Folder already exists", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void createNewFile() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Text File");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fileName = input.getText().toString().trim();
                if (!fileName.isEmpty()) {
                    // Add .txt extension if not provided
                    if (!fileName.contains(".")) {
                        fileName += ".txt";
                    }

                    File newFile = new File(currentDirectory, fileName);
                    if (!newFile.exists()) {
                        try {
                            if (newFile.createNewFile()) {
                                Toast.makeText(MainActivity.this, "File created", Toast.LENGTH_SHORT).show();
                                loadFileList();
                            } else {
                                Toast.makeText(MainActivity.this, "Failed to create file", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "File already exists", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "File name cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void renameFile(final File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename");

        final EditText input = new EditText(this);
        input.setText(file.getName());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    File newFile = new File(file.getParentFile(), newName);
                    if (!newFile.exists()) {
                        if (file.renameTo(newFile)) {
                            Toast.makeText(MainActivity.this, "Renamed successfully", Toast.LENGTH_SHORT).show();
                            loadFileList();
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to rename", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "A file with that name already exists", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void deleteFile(final File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete");
        builder.setMessage("Are you sure you want to delete " + file.getName() + "?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (deleteRecursive(file)) {
                    Toast.makeText(MainActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                    loadFileList();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return fileOrDirectory.delete();
    }

    private void copyFile(File src, File destDir) {
        try {
            File dest = new File(destDir, src.getName());

            // Check if destination file already exists
            if (dest.exists()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("File Already Exists");
                builder.setMessage("Do you want to replace the existing file?");

                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executeCopy(src, dest);
                    }
                });

                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.show();

            } else {
                executeCopy(src, dest);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error copying file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void executeCopy(File src, File dest) {
        try {
            if (src.isDirectory()) {
                // Create destination directory
                if (!dest.exists()) {
                    dest.mkdirs();
                }

                // Copy all contents
                String[] files = src.list();
                if (files != null) {
                    for (String file : files) {
                        File srcFile = new File(src, file);
                        File destFile = new File(dest, file);
                        executeCopy(srcFile, destFile);
                    }
                }
            } else {
                // Copy the file
                FileChannel sourceChannel = null;
                FileChannel destChannel = null;

                try {
                    sourceChannel = new FileInputStream(src).getChannel();
                    destChannel = new FileOutputStream(dest).getChannel();
                    destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());

                } finally {
                    if (sourceChannel != null) sourceChannel.close();
                    if (destChannel != null) destChannel.close();
                }
            }

            Toast.makeText(this, "Copied successfully", Toast.LENGTH_SHORT).show();
            loadFileList();

        } catch (IOException e) {
            Toast.makeText(this, "Error copying: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void moveFile(File src, File destDir) {
        try {
            File dest = new File(destDir, src.getName());

            // Check if destination file already exists
            if (dest.exists()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("File Already Exists");
                builder.setMessage("Do you want to replace the existing file?");

                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dest.delete();
                        executeMove(src, dest);
                    }
                });

                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.show();

            } else {
                executeMove(src, dest);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error moving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void executeMove(File src, File dest) {
        // Try simple rename/move first
        if (src.renameTo(dest)) {
            Toast.makeText(this, "Moved successfully", Toast.LENGTH_SHORT).show();
            loadFileList();
            return;
        }

        // If direct move fails, try copy and delete
        try {
            copyFile(src, dest.getParentFile());
            if (deleteRecursive(src)) {
                Toast.makeText(this, "Moved successfully", Toast.LENGTH_SHORT).show();
                loadFileList();
            } else {
                Toast.makeText(this, "File copied but original could not be deleted", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error moving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (!currentDirectory.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            navigateToDirectory(currentDirectory.getParentFile());
        } else {
            super.onBackPressed();
        }
    }
}