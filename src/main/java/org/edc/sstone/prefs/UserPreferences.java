/*
 * Copyright (c) 2012 EDC
 * 
 * This file is part of Stepping Stone.
 * 
 * Stepping Stone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Stepping Stone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Stepping Stone.  If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */
package org.edc.sstone.prefs;

import java.io.UnsupportedEncodingException;

import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

import org.apache.commons.lang.IntHashMap;
import org.edc.sstone.CheckedException;
import org.edc.sstone.Constants;
import org.edc.sstone.log.Log;
import org.edc.sstone.store.ValueSource;
import org.edc.sstone.util.StdLib;

/**
 * @author Greg Orlowski
 */
public class UserPreferences implements ValueSource {

    private RecordStore rms = null;
    private static final String RMS_NAME = "sstone";

    /**
     * The keys are our recordIds (business key) and the values are rms record ids (system key)
     */
    private IntHashMap recordIdMapping;

    private void updateRecordIndex(boolean closeAfterUpdate) throws RecordStoreException {
        if (recordIdMapping == null) {
            recordIdMapping = new IntHashMap();
        }
        try {
            open();
            RecordEnumeration re = rms.enumerateRecords(null, null, false);
            while (re.hasNextElement()) {
                int rmsRecordId = re.nextRecordId();
                byte[] record = rms.getRecord(rmsRecordId);
                recordIdMapping.put(getRecordId(record), new Integer(rmsRecordId));
            }
        } finally {
            if (closeAfterUpdate)
                close();
        }
    }

    private int getRecordId(byte[] record) {
        return StdLib.bytesToInt(record, 0);
    }

    protected Object getRecordValue(byte[] record) {
        int recordId = getRecordId(record);
        byte recordType = getRecordType(recordId);
        Object ret = null;

        switch (recordType) {
            case Constants.TYPE_INT:
                ret = new Integer(StdLib.bytesToInt(record, 4));
                break;
            case Constants.TYPE_STRING:
                try {
                    ret = new String(record, 4, record.length - 4, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    handleUnsupportedEncoding(e);
                }
                break;
        }
        return ret;
    }

    protected byte[] makeRecord(int id, Object value) {
        byte[] valueBytes = null;
        if (value instanceof Integer) {
            valueBytes = StdLib.intToBytes(((Integer) value).intValue());
        } else if (value instanceof String) {
            try {
                valueBytes = ((String) value).getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                handleUnsupportedEncoding(e);
            }
        }
        byte[] ret = new byte[valueBytes.length + 4];
        StdLib.copyIntInto(id, ret, 0);
        System.arraycopy(valueBytes, 0, ret, 4, valueBytes.length);
        return ret;
    }

    protected void open() throws RecordStoreException {
        rms = RecordStore.openRecordStore(RMS_NAME, true, RecordStore.AUTHMODE_ANY, true);
        if (recordIdMapping == null) {
            updateRecordIndex(false);
        }
    }

    protected void close() {
        if (rms != null) {
            try {
                rms.closeRecordStore();
            } catch (RecordStoreException failedToCloseRmsException) {
                Log.warn("Failed to close RMS: ", failedToCloseRmsException);
            } finally {
                rms = null;
            }
        }
    }

    public Object getValue(int recordId) throws CheckedException {
        try {
            open();
            Object rmsRecordIdObj = recordIdMapping.get(recordId);
            if (rmsRecordIdObj == null)
                return null;
            byte[] record = rms.getRecord(((Integer) rmsRecordIdObj).intValue());
            return getRecordValue(record);
        } catch (RecordStoreException e) {
            Log.warn("Record store exception", e);
            throw new CheckedException(CheckedException.PREF_DB_READ_ERROR);
        } finally {
            close();
        }
    }

    protected void handleUnsupportedEncoding(UnsupportedEncodingException e) {
        Log.warn("UTF-8 encoding not supported", e);
        // TODO: replace String UnsupportedEncodingException (UTF-8) with error screen
        // having said that, I doubt any target devices would ever throw this.
        throw new RuntimeException(e.getMessage());
    }

    /**
     * The default implementation always returns the int type b/c we we do not need string records.
     * 
     * @param recordId
     *            the id of the record in the store
     * @return {@link Constants#TYPE_INT} or {@link Constants#TYPE_STRING}
     */
    protected byte getRecordType(int recordId) {
        return Constants.TYPE_INT;
    }

    /**
     * Maybe this is a hack to just cast value to a byte, but so far I do not need more than 1 byte
     * for any value.
     * 
     * @param recordId
     * @param value
     */
    public void setValue(int recordId, Object value) throws CheckedException {
        byte[] record = makeRecord(recordId, value);
        try {
            open();
            if (!recordIdMapping.containsKey(recordId)) {
                int rmsRecordId = rms.addRecord(record, 0, record.length);
                recordIdMapping.put(recordId, new Integer(rmsRecordId));
            } else {
                int rmsRecordId = ((Integer) recordIdMapping.get(recordId)).intValue();
                rms.setRecord(rmsRecordId, record, 0, record.length);
            }
        } catch (RecordStoreException e) {
            throw new CheckedException(e, CheckedException.PREF_DB_WRITE_ERROR);
        } finally {
            close();
        }
    }
}
