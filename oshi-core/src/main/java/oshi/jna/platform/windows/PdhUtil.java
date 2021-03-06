/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.jna.platform.windows;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Memory; // NOSONAR squid:S1191
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinReg;

/**
 * Pdh utility API.
 * 
 * @author widdis[at]gmail[dot]com
 */
public abstract class PdhUtil {
    private static final int CHAR_TO_BYTES = Boolean.getBoolean("w32.ascii") ? 1 : Native.WCHAR_SIZE;

    // This REG_MULTI_SZ value in HKLM provides English counters regardless of
    // the current locale setting
    private static final String ENGLISH_COUNTER_KEY = "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Perflib\\009";
    private static final String ENGLISH_COUNTER_VALUE = "Counter";

    /**
     * Utility method to call Pdh's PdhLookupPerfNameByIndex that allocates the
     * required memory for the szNameBuffer parameter based on the type mapping
     * used, calls to PdhLookupPerfNameByIndex, and returns the received string.
     * 
     * @param szMachineName
     *            Null-terminated string that specifies the name of the computer
     *            where the specified performance object or counter is located.
     *            The computer name can be specified by the DNS name or the IP
     *            address. If NULL, the function uses the local computer.
     * @param dwNameIndex
     *            Index of the performance object or counter.
     * @return Returns the name of the performance object or counter.
     */
    public static String PdhLookupPerfNameByIndex(String szMachineName, int dwNameIndex) {
        // Call once to get required buffer size
        DWORDByReference pcchNameBufferSize = new DWORDByReference(new DWORD(0));
        int result = Pdh.INSTANCE.PdhLookupPerfNameByIndex(szMachineName, dwNameIndex, null, pcchNameBufferSize);
        if (result != WinError.ERROR_SUCCESS && result != Pdh.PDH_MORE_DATA) {
            throw new PdhException(result);
        }

        // Can't allocate 0 memory
        if (pcchNameBufferSize.getValue().longValue() < 1) {
            return "";
        }
        // Allocate buffer and call again
        Memory mem = new Memory(pcchNameBufferSize.getValue().longValue() * CHAR_TO_BYTES);
        result = Pdh.INSTANCE.PdhLookupPerfNameByIndex(szMachineName, dwNameIndex, mem, pcchNameBufferSize);

        if (result != WinError.ERROR_SUCCESS) {
            throw new PdhException(result);
        }

        // Convert buffer to Java String
        if (CHAR_TO_BYTES == 1) {
            return mem.getString(0);
        } else {
            return mem.getWideString(0);
        }
    }

    /**
     * Utility method similar to Pdh's PdhLookupPerfIndexByName that returns the
     * counter index corresponding to the specified counter name in English.
     * Uses the registry on the local machine to find the index in the English
     * locale, regardless of the current language setting on the machine.
     * 
     * @param szNameBuffer
     *            The English name of the performance counter
     * @return The counter's index if it exists, or 0 otherwise.
     */
    public static int PdhLookupPerfIndexByEnglishName(String szNameBuffer) {
        // Look up list of english names and ids
        String[] counters = Advapi32Util.registryGetStringArray(WinReg.HKEY_LOCAL_MACHINE, ENGLISH_COUNTER_KEY,
                ENGLISH_COUNTER_VALUE);
        // Array contains alternating index/name pairs
        // {"1", "1847", "2", "System", "4", "Memory", ... }
        // Get position of name in the array (odd index), return parsed value of
        // previous even index
        for (int i = 1; i < counters.length; i += 2) {
            if (counters[i].equals(szNameBuffer)) {
                try {
                    return Integer.parseInt(counters[i - 1]);
                } catch (NumberFormatException e) {
                    // Unexpected but handle anyway
                    return 0;
                }
            }
        }
        // Didn't find the String
        return 0;
    }

    /**
     * Utility method to call Pdh's PdhEnumObjectItems that allocates the
     * required memory for the lists parameters based on the type mapping used,
     * calls to PdhEnumObjectItems, and returns the received lists of strings.
     * 
     * @param szDataSource
     *            String that specifies the name of the log file used to
     *            enumerate the counter and instance names. If NULL, the
     *            function uses the computer specified in the szMachineName
     *            parameter to enumerate the names.
     * @param szMachineName
     *            String that specifies the name of the computer that contains
     *            the counter and instance names that you want to enumerate.
     *            Include the leading slashes in the computer name, for example,
     *            \\computername. If the szDataSource parameter is NULL, you can
     *            set szMachineName to NULL to specify the local computer.
     * @param szObjectName
     *            String that specifies the name of the object whose counter and
     *            instance names you want to enumerate.
     * @param dwDetailLevel
     *            Detail level of the performance items to return. All items
     *            that are of the specified detail level or less will be
     *            returned.
     * @return Returns a List of Strings of the counters for the object.
     */
    public static PdhEnumObjectItems PdhEnumObjectItems(String szDataSource, String szMachineName, String szObjectName,
            int dwDetailLevel) {
        List<String> counters = new ArrayList<>();
        List<String> instances = new ArrayList<>();

        // Call once to get string lengths
        DWORDByReference pcchCounterListLength = new DWORDByReference(new DWORD(0));
        DWORDByReference pcchInstanceListLength = new DWORDByReference(new DWORD(0));
        int result = Pdh.INSTANCE.PdhEnumObjectItems(szDataSource, szMachineName, szObjectName, null,
                pcchCounterListLength, null, pcchInstanceListLength, dwDetailLevel, 0);
        if (result != WinError.ERROR_SUCCESS && result != Pdh.PDH_MORE_DATA) {
            throw new PdhException(result);
        }

        Memory mszCounterList = null;
        Memory mszInstanceList = null;

        if (pcchCounterListLength.getValue().longValue() > 0) {
            mszCounterList = new Memory(pcchCounterListLength.getValue().longValue() * CHAR_TO_BYTES);
        }

        if (pcchInstanceListLength.getValue().longValue() > 0) {
            mszInstanceList = new Memory(pcchInstanceListLength.getValue().longValue() * CHAR_TO_BYTES);
        }

        result = Pdh.INSTANCE.PdhEnumObjectItems(szDataSource, szMachineName, szObjectName, mszCounterList,
                pcchCounterListLength, mszInstanceList, pcchInstanceListLength, dwDetailLevel, 0);

        if (result != WinError.ERROR_SUCCESS) {
            throw new PdhException(result);
        }

        // Fetch counters
        if (mszCounterList != null) {
            int offset = 0;
            while (offset < mszCounterList.size()) {
                String s = null;
                if (CHAR_TO_BYTES == 1) {
                    s = mszCounterList.getString(offset);
                } else {
                    s = mszCounterList.getWideString(offset);
                }
                // list ends with double null
                if (s.isEmpty()) {
                    break;
                }
                counters.add(s);
                // Increment for string + null terminator
                offset += (s.length() + 1) * CHAR_TO_BYTES;
            }
        }

        if (mszInstanceList != null) {
            int offset = 0;
            while (offset < mszInstanceList.size()) {
                String s = null;
                if (CHAR_TO_BYTES == 1) {
                    s = mszInstanceList.getString(offset);
                } else {
                    s = mszInstanceList.getWideString(offset);
                }
                // list ends with double null
                if (s.isEmpty()) {
                    break;
                }
                instances.add(s);
                // Increment for string + null terminator
                offset += (s.length() + 1) * CHAR_TO_BYTES;
            }
        }

        return new PdhEnumObjectItems(counters, instances);
    }

    /**
     * Holder Object for PdhEnumObjectsItems. The embedded lists are modifiable
     * lists and can be accessed through the {@link #getCounters()} and
     * {@link #getInstances()} accessors.
     */
    public static class PdhEnumObjectItems {
        private final List<String> counters;
        private final List<String> instances;

        public PdhEnumObjectItems(List<String> counters, List<String> instances) {
            this.counters = copyAndEmptyListForNullList(counters);
            this.instances = copyAndEmptyListForNullList(instances);
        }

        /**
         * @return the embedded counters list, all calls to this function
         *         receive the same list and thus share modifications
         */
        public List<String> getCounters() {
            return counters;
        }

        /**
         * @return the embedded instances list, all calls to this function
         *         receive the same list and thus share modifications
         */
        public List<String> getInstances() {
            return instances;
        }

        private List<String> copyAndEmptyListForNullList(List<String> inputList) {
            if (inputList == null) {
                return new ArrayList<>();
            } else {
                return new ArrayList<>(inputList);
            }
        }

        @Override
        public String toString() {
            return "PdhEnumObjectItems{" + "counters=" + counters + ", instances=" + instances + '}';
        }
    }

    @SuppressWarnings("serial")
    public static final class PdhException extends RuntimeException {
        private final int errorCode;

        public PdhException(int errorCode) {
            super(String.format("Pdh call failed with error code 0x%08X", errorCode));
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }
    }
}