package com.octo.android.robospice.persistence.springandroid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Application;
import android.text.TextUtils;

import com.octo.android.robospice.persistence.exception.CacheCreationException;
import com.octo.android.robospice.persistence.exception.CacheLoadingException;
import com.octo.android.robospice.persistence.exception.CacheSavingException;
import com.octo.android.robospice.persistence.file.InFileObjectPersister;
import com.octo.android.robospice.utils.CharEncoding;
import com.octo.android.robospice.utils.FileUtils;

import roboguice.util.temp.Ln;

public abstract class SpringAndroidObjectPersister<T> extends InFileObjectPersister<T> {

    // ============================================================================================
    // CONSTRUCTOR
    // ============================================================================================
    public SpringAndroidObjectPersister(Application application, Class<T> clazz, File cacheFolder) throws CacheCreationException {
        super(application, clazz, cacheFolder);
    }

    public SpringAndroidObjectPersister(Application application, Class<T> clazz) throws CacheCreationException {
        super(application, clazz);
    }

    // ============================================================================================
    // METHODS
    // ============================================================================================

    @Override
    protected T readCacheDataFromFile(File file) throws CacheLoadingException {
        try {
            String resultJson = null;
            synchronized (file.getAbsolutePath().intern()) {
                resultJson = FileUtils.readFileToString(file, CharEncoding.UTF_8);
            }
            if (!TextUtils.isEmpty(resultJson)) {
                T result = deserializeData(resultJson);
                return result;
            }
            throw new CacheLoadingException("Unable to restore cache content : cache file is empty");
        } catch (FileNotFoundException e) {
            // Should not occur (we test before if file exists)
            // Do not throw, file is not cached
            Ln.w("file " + file.getAbsolutePath() + " does not exists", e);
            return null;
        } catch (CacheLoadingException e) {
            throw e;
        } catch (Exception e) {
            throw new CacheLoadingException(e);
        }
    }

    protected abstract T deserializeData(String json) throws CacheLoadingException;

    @Override
    public T saveDataToCacheAndReturnData(final T data, final Object cacheKey) throws CacheSavingException {

        try {
            if (isAsyncSaveEnabled()) {
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        try {
                            saveData(data, cacheKey);
                        } catch (IOException e) {
                            Ln.e(e, "An error occured on saving request " + cacheKey + " data asynchronously");
                        } catch (CacheSavingException e) {
                            Ln.e(e, "An error occured on saving request " + cacheKey + " data asynchronously");
                        }
                    };
                };
                t.start();
            } else {
                saveData(data, cacheKey);
            }
        } catch (CacheSavingException e) {
            throw e;
        } catch (Exception e) {
            throw new CacheSavingException(e);
        }
        return data;
    }

    protected abstract void saveData(T data, Object cacheKey) throws IOException, CacheSavingException;
}
