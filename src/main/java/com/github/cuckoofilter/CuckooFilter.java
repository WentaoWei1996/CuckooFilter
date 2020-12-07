package com.github.cuckoofilter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/**
 * @Author: wwt
 * @Date: 2020/11/28 15:52
 */
public class CuckooFilter {

    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final int MAX_NUM_KICKS = 500;
    private static final int FINGERPRINT_LEN = 1;
    private int capacity;
    private int size = 0;
    private Bucket[] buckets;
    private Random random;

    public CuckooFilter(int capacity) {
        capacity = tableSizeFor(capacity);
        this.capacity = capacity;
        buckets = new Bucket[capacity];
        random = new Random();
        for (int i = 0; i < capacity; i++) {
            buckets[i] = new Bucket();
        }
    }


    /**
     * insert an object into cuckoo filter
     *
     * @param o The object is to be inserted
     * @return false if filter consideredly full or insert an null object
     */
    public boolean insert(Object o, double value) {
        if (o == null)
            return false;
        Fingerprint f = fingerprint(o, FINGERPRINT_LEN);
        int i1 = hash(o);
        int i2 = i1 ^ hash(f);

        if (buckets[i1].insert(f, value) || buckets[i2].insert(f, value)) {
            size++;
            return true;
        }
        // must relocate existing items
        return relocateAndInsert(i1, i2, f, value);
    }

    public double get(Object o) {
        if (o == null)
            return 0.0;

        Fingerprint f = fingerprint(o, FINGERPRINT_LEN);
        int i1 = hash(o);
        int i2 = i1 ^ hash(f);

        return Math.max(buckets[i1].get(f), buckets[i2].get(f));
    }

    /**
     * insert an object into cuckoo filter before checking whether the object is already inside
     *
     * @param o The object is to be inserted
     * @return false when filter consideredly full or the object is already inside
     */
    public boolean insertUnique(Object o, double value) {
        if (o == null || contains(o))
            return false;
        return insert(o, value);
    }


    private boolean relocateAndInsert(int i1, int i2, Fingerprint f, double value) {
        boolean flag = random.nextBoolean();
        int itemp = flag ? i1 : i2;
        for (int i = 0; i < MAX_NUM_KICKS; i++) {
            int position = random.nextInt(Bucket.BUCKET_SIZE);
            f = buckets[itemp].swap(position, f, value);
            itemp = itemp ^ hash(f);
            if (buckets[itemp].insert(f, value)) {
                size++;
                return true;
            }
        }
        return false;
    }


    /**
     * Returns <tt>true</tt> if this filter contains a fingerprint for the
     * object.
     *
     * @param o The object is to be tested
     * @return <tt>true</tt> if this map contains a fingerprint for the
     * object.
     */
    public boolean contains(Object o) {
        if(o == null)
            return false;
        Fingerprint f = fingerprint(o, FINGERPRINT_LEN);
        int i1 = hash(o);
        int i2 = i1 ^ hash(f);
        return buckets[i1].contains(f) || buckets[i2].contains(f);
    }

    /**
     * delete object from cuckoo filter.Note that, to delete an item x safely, it must have been
     * previously inserted.
     *
     * @param o 变量
     * @return <tt>true</tt> if this map contains a fingerprint for the
     * object.
     */
    public boolean delete(Object o) {
        if(o == null)
            return false;
        Fingerprint f = fingerprint(o, FINGERPRINT_LEN);
        int i1 = hash(o);
        int i2 = i1 ^ hash(f);
        return buckets[i1].delete(f) || buckets[i2].delete(f);
    }

    /**
     * Returns the number of fingerprints in this map.
     *
     * @return the number of fingerprints in this map
     */
    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public Fingerprint fingerprint(Object key, int len) {

        Fingerprint fingerprint = new Fingerprint(len);

        byte[] fs = new byte[len];

        int h = key.hashCode();

        for (int i = 0; i < len; i++) {
            h += ~(h << 15);
            h ^= (h >> 10);
            h += (h << 3);
            h ^= (h >> 6);
            h += ~(h << 11);
            h ^= (h >> 16);

            byte hash = (byte) h;
            if (hash == 0) {
                hash = 40;
            }

            fs[i] = hash;
        }

        fingerprint.setFingerprint(fs);

        return fingerprint;

    }

    public int hash(Object key) {
        int h = key.hashCode();
        h -= (h << 6);
        h ^= (h >> 17);
        h -= (h << 9);
        h ^= (h << 4);
        h -= (h << 3);
        h ^= (h << 10);
        h ^= (h >> 15);
        return h & (capacity - 1);
    }


    static int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    static class Bucket {

        public static final int FINGERPRINT_SIZE = 1;
        public static final int BUCKET_SIZE = 4;
        public static final byte NULL_FINGERPRINT = 0;

        private final Slot[] slots = new Slot[BUCKET_SIZE];

        public Bucket() {
            for (int i = 0; i < BUCKET_SIZE; i++) {
                this.slots[i] = new Slot(FINGERPRINT_SIZE);
            }
        }

        public boolean insert(Fingerprint fingerprint, double value) {
            for (Slot slot : slots) {
                if (slot.getFingerprint().getFingerprint()[0] == NULL_FINGERPRINT) {
                    slot.setFingerprint(fingerprint);
                    slot.setFlag(false);
                    slot.setValue(value);
                    return true;
                }
            }
            return false;
        }

        public double get(Fingerprint fingerprint) {

            for (Slot slot : slots) {
                if (slot.getFingerprint().equals(fingerprint)) {
                    return slot.getValue();
                }
            }

            return 0.0;
        }

        public boolean delete(Fingerprint fingerprint) {
            for (Slot slot : slots) {
                if (slot.getFingerprint().equals(fingerprint)) {
                    slot.setFingerprint(new Fingerprint(FINGERPRINT_SIZE));
                    slot.setFlag(true);
                    slot.setValue(0.0);
                    return true;
                }
            }
            return false;
        }

        public boolean contains(Fingerprint fingerprint) {
            for (Slot slot : slots) {
                if (slot.getFingerprint().equals(fingerprint))
                    return true;
            }
            return false;
        }

        public Fingerprint swap(int position, Fingerprint fingerprint, double value) {
            Fingerprint tmpfg = slots[position].getFingerprint();
            slots[position].setFingerprint(fingerprint);
            slots[position].setFlag(false);
            slots[position].setValue(value);
            return tmpfg;
        }
    }

    public static class Slot {

        private Fingerprint fingerprint;
        private boolean flag;
        private double value;

        public Slot(int fingerprintSize) {
            this.fingerprint = new Fingerprint(fingerprintSize);
            this.flag = true;
            this.value = 0.0;
        }

        public Slot(Fingerprint fingerprint, double value) {
            this.fingerprint = fingerprint;
            this.flag = false;
            this.value = value;
        }

        public void setFingerprint(Fingerprint f) {
            this.fingerprint = f;
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public Fingerprint getFingerprint() {
            return this.fingerprint;
        }

        public boolean getFlag() {
            return this.flag;
        }

        public double getValue() {
            return this.value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Slot)) return false;
            Slot slot = (Slot) o;
            return getFingerprint() == slot.getFingerprint();
        }

        @Override
        public int hashCode() {
            return Objects.hash(getFingerprint());
        }
    }

    public static class Fingerprint {

        private byte[] fingerprint;
        private int size;

        public Fingerprint(int size) {
            this.size = size;
            this.fingerprint = new byte[size];
            for (int i = 0; i < size; i++) {
                fingerprint[i] = 0;
            }
        }

        public byte[] getFingerprint() {
            return fingerprint;
        }

        public void setFingerprint(byte[] fingerprint) {
            this.fingerprint = fingerprint;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Fingerprint)) return false;
            Fingerprint that = (Fingerprint) o;
            return Arrays.equals(getFingerprint(), that.getFingerprint());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(getFingerprint());
        }
    }
}