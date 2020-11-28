package com.wwt.cuckoofilter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/**
 * @Author: wwt
 * @Date: 2020/11/27 20:52
 */
public class CuckooFilter {

    public static void main(String[] args) {
        CuckooFilter cuckooFilter = new CuckooFilter(1 << 4);
        cuckooFilter.insert("wwt", 100);
        cuckooFilter.insert("ksdjkf", 239);
        System.out.println(cuckooFilter.get("wwt"));
        System.out.println(cuckooFilter.get("ksdjkf"));
    }

    private Bucket[] buckets;
    private int capacity;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final int MAX_NUM_KICKS = 500;
    private static final int FINGERPRINT_LEN = 1;
    private Random random;
    private int size;

    public CuckooFilter(int capacity) {
        this.random = new Random();
        capacity = tableSizeFor(capacity);
        this.capacity = capacity;
        this.size = 0;
        this.buckets = new Bucket[capacity];
        for (int i = 0; i < capacity; i++) {
            this.buckets[i] = new Bucket();
        }
    }

    public boolean insert(String key, double value) {

        Fingerprint f = fingerprint(key, FINGERPRINT_LEN);
        int p1 = hash(key);
        int p2 = p1 ^ hash(f);

        if (!this.buckets[p1].isFull() && this.buckets[p1].add(f, value)) {
            this.size++;
            return true;
        }

        if (!this.buckets[p2].isFull() && this.buckets[p2].add(f, value)) {
            this.size++;
            return true;
        }

        return relocateAndInsert(p1, p2, f, value);
    }

    public double get(String key) {

        Fingerprint f = fingerprint(key, FINGERPRINT_LEN);
        int p1 = hash(key);
        int p2 = p1 ^ hash(f);

        return Math.max(this.buckets[p1].get(f), this.buckets[p2].get(f));
    }

    public boolean contains(String key) {

        Fingerprint f = fingerprint(key, FINGERPRINT_LEN);
        int p1 = hash(key);
        int p2 = p1 ^ hash(f);

        return this.buckets[p1].contains(f) || this.buckets[p2].contains(f);
    }

    private boolean relocateAndInsert(int p1, int p2, Fingerprint f, double value) {

        boolean flag = this.random.nextBoolean();
        int replacedBucketPosition = flag ? p1 : p2;

        for (int i = 0; i < MAX_NUM_KICKS; i++) {
            //随机得到被替换bucket中slot的位置
            int replacedSlotPosition = this.random.nextInt(Bucket.BUCKET_SIZE);
            //获取被替换slot中的key和value
            Fingerprint replacedF = this.buckets[replacedBucketPosition].slots[replacedSlotPosition].getFingerprint();
            double replaceValue = this.buckets[replacedBucketPosition].slots[replacedSlotPosition].getValue();
            //将f和value插入带替换slot
            this.buckets[replacedBucketPosition].slots[replacedSlotPosition].setFingerprint(f);
            this.buckets[replacedBucketPosition].slots[replacedSlotPosition].setValue(value);

            //获取被替换f的对偶位置
            replacedBucketPosition = replacedSlotPosition ^ hash(replacedF);

            if (!this.buckets[replacedBucketPosition].isFull() && this.buckets[replacedBucketPosition].add(replacedF, replaceValue)) {
                this.size++;
                return true;
            }
        }

        return false;
    }

    public static class Bucket{

        private Slot[] slots;
        public static final int BUCKET_SIZE = 4;
        public static final byte NULL_FINGERPRINT = 0;

        public Bucket() {
            this.slots = new Slot[BUCKET_SIZE];
            for (int i = 0; i < BUCKET_SIZE; i++) {
                this.slots[i] = new Slot();
            }
        }

        public boolean isFull() {

            for (int i = 0; i < BUCKET_SIZE; i++) {
                if (this.slots[i].getFlag()) {
                    return false;
                }
            }

            return true;
        }

        public boolean add(Fingerprint key, double value) {

            for (int i = 0; i < BUCKET_SIZE; i++) {
                if (this.slots[i].getFlag()) {
                    this.slots[i].setFingerprint(key);
                    this.slots[i].setFlag(false);
                    this.slots[i].setValue(value);
                    return true;
                }
            }

            return false;
        }

        public double get(Fingerprint f) {

            for (int i = 0; i < BUCKET_SIZE; i++) {
                if (this.slots[i].getFingerprint().equals(f)) {
                    return this.slots[i].getValue();
                }
            }

            return 0.0;
        }

        public boolean contains(Fingerprint f) {

            for (int i = 0; i < BUCKET_SIZE; i++) {
                if (this.slots[i].getFingerprint().equals(f)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static class Slot {

        private Fingerprint fingerprint;
        private boolean flag;
        private double value;

        public Slot() {
            this.fingerprint = new Fingerprint(2);
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

    public Fingerprint fingerprint(String key, int len) {

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
            if (hash == Bucket.NULL_FINGERPRINT) {
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

        return h & (this.capacity - 1);
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
}
