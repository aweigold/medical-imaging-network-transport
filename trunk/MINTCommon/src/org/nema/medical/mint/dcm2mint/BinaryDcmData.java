/*
 *   Copyright 2010 MINT Working Group
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.nema.medical.mint.dcm2mint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomInputStream;

/**
 * Not thread-safe, due to contained mutable cache objects.
 *
 * @author Uli Bubenheimer
 *
 */
public final class BinaryDcmData implements BinaryData {
    private static final class FileTagpath {
        public FileTagpath(final File dcmFile, final int[] tagPath) {
            this.dcmFile = dcmFile;
            this.tagPath = tagPath;
        }

        public final File dcmFile;
        public final int[] tagPath;
    }

    private final List<FileTagpath> binaryItems = new ArrayList<FileTagpath>();

    private DicomObject cachedRootDicomObject;
    private File cachedRootDicomObjectFile;

    private class BinaryItemStream extends InputStream {
        public BinaryItemStream(final FileTagpath fileTagPath) {
            this.tagPath = fileTagPath;
        }

        @Override
        public int read() throws IOException {
            byte[] binaryItem = binaryItemRef.get();
            if (binaryItem == null) {
                binaryItem = fileTagpathToFile(tagPath);
                binaryItemRef = new WeakReference<byte[]>(binaryItem);
            }
            if (pos >= binaryItem.length) {
                return -1;
            }

            //Convert to char first, so that negative byte values do not become negative int values
            return (char) binaryItem[pos++];
        }

        private Reference<byte[]> binaryItemRef = new WeakReference<byte[]>(null);
        private final FileTagpath tagPath;
        private int pos = 0;
    }

    @Override
    public void add(final File dcmFile, final int[] tagPath, final DicomElement dcmElem) {
        final int[] newTagPath = new int[tagPath.length + 1];
        System.arraycopy(tagPath, 0, newTagPath, 0, tagPath.length);
        newTagPath[tagPath.length] = dcmElem.tag();
        final FileTagpath storeElem = new FileTagpath(dcmFile, newTagPath);
        binaryItems.add(storeElem);
    }

    @Override
    public byte[] getBinaryItem(final int index) {
        return fileTagpathToFile(binaryItems.get(index));
    }

    public InputStream getBinaryItemStream(final int index) {
        return new BinaryItemStream(binaryItems.get(index));
    }

    public Iterator<InputStream> streamIterator() {
        return new Iterator<InputStream>() {

            @Override
            public boolean hasNext() {
                return itemIterator.hasNext();
            }

            @Override
            public InputStream next() {
                return new BinaryItemStream(itemIterator.next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            private final Iterator<FileTagpath> itemIterator = binaryItems.iterator();
        };
    }

    @Override
    public int size() {
        return binaryItems.size();
    }

    @Override
    public Iterator<byte[]> iterator() {
        return new Iterator<byte[]>() {

            @Override
            public boolean hasNext() {
                return itemIterator.hasNext();
            }

            @Override
            public byte[] next() {
                return fileTagpathToFile(itemIterator.next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            private final Iterator<FileTagpath> itemIterator = binaryItems.iterator();
        };
    }

    private byte[] fileTagpathToFile(final FileTagpath binaryItemPath) {
        final File targetDcmFile = binaryItemPath.dcmFile;
        if (!targetDcmFile.equals(cachedRootDicomObjectFile)) {
            final DicomObject newRootDicomObject;
            try {
                final DicomInputStream stream = new DicomInputStream(targetDcmFile);
                try {
                    newRootDicomObject = stream.readDicomObject();
                } finally {
                    stream.close();
                }
            } catch(final IOException e) {
                throw new RuntimeException(e);
            }

            cachedRootDicomObject = newRootDicomObject;
            cachedRootDicomObjectFile = targetDcmFile;
        }
        return cachedRootDicomObject.getBytes(binaryItemPath.tagPath);
    }
}
