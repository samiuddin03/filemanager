package com.example.filemanager;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileAdapter extends ArrayAdapter<File> {

    private Context context;
    private List<File> fileList;

    public FileAdapter(Context context, List<File> files) {
        super(context, R.layout.file_item, files);
        this.context = context;
        this.fileList = files;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.file_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.fileName = convertView.findViewById(R.id.file_name);
            viewHolder.fileDetails = convertView.findViewById(R.id.file_details);
            viewHolder.fileIcon = convertView.findViewById(R.id.file_icon);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        File file = fileList.get(position);

        if (file != null) {
            String fileName = file.getName();

            // Check if this is the parent directory
            if (position == 0 && file.getAbsolutePath().equals(file.getParentFile().getAbsolutePath())) {
                fileName = "..";
                viewHolder.fileDetails.setText("Parent Directory");
                viewHolder.fileIcon.setImageResource(R.drawable.ic_folder);
            } else {
                // Set file name
                viewHolder.fileName.setText(fileName);

                // Format file details
                if (file.isDirectory()) {
                    int itemCount = 0;
                    File[] contents = file.listFiles();
                    if (contents != null) {
                        itemCount = contents.length;
                    }
                    viewHolder.fileDetails.setText(itemCount + " items | " + formatDate(file.lastModified()));
                    viewHolder.fileIcon.setImageResource(R.drawable.ic_folder);
                } else {
                    viewHolder.fileDetails.setText(formatFileSize(file.length()) + " | " + formatDate(file.lastModified()));
                    viewHolder.fileIcon.setImageResource(getFileIconResource(file));
                }
            }
        }

        return convertView;
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private String formatDate(long timeMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timeMillis));
    }

    private int getFileIconResource(File file) {
        String fileName = file.getName().toLowerCase();

        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") ||
                fileName.endsWith(".gif") || fileName.endsWith(".bmp")) {
            return R.drawable.ic_image;
        } else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".ogg") ||
                fileName.endsWith(".flac") || fileName.endsWith(".aac")) {
            return R.drawable.ic_audio;
        } else if (fileName.endsWith(".mp4") || fileName.endsWith(".3gp") || fileName.endsWith(".mkv") ||
                fileName.endsWith(".avi") || fileName.endsWith(".mov")) {
            return R.drawable.ic_video;
        } else if (fileName.endsWith(".pdf")) {
            return R.drawable.ic_pdf;
        } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx") || fileName.endsWith(".txt") ||
                fileName.endsWith(".rtf") || fileName.endsWith(".odt")) {
            return R.drawable.ic_document;
        } else if (fileName.endsWith(".zip") || fileName.endsWith(".rar") || fileName.endsWith(".7z") ||
                fileName.endsWith(".tar") || fileName.endsWith(".gz")) {
            return R.drawable.ic_archive;
        } else if (fileName.endsWith(".apk")) {
            return R.drawable.ic_apk;
        } else {
            return R.drawable.ic_file;
        }
    }

    static class ViewHolder {
        TextView fileName;
        TextView fileDetails;
        ImageView fileIcon;
    }
}