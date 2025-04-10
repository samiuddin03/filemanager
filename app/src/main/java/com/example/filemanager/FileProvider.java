package com.example.filemanager;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;

public class FileProvider extends androidx.core.content.FileProvider {
    // This class extends the built-in FileProvider and can be used to add custom functionality if needed
}