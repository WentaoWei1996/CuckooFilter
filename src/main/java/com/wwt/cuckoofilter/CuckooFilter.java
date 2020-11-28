package com.wwt.cuckoofilter;

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
    private static final int MAX_NUM_KICKS = 500;
    private Random random;
    private int size;

    public CuckooFilter(int capacity) {
        this.random = new Random();
        this.capacity = capacity;
        this.size = 0;
        this.buckets = new Bucket[capacity];
        for (int i = 0; i < capacity; i++) {
            this.buckets[i] = new Bucket();
        }
    }

    public boolean insert(String key, double value) {

        byte f = fingerprint(key);
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

        byte f = fingerprint(key);
        int p1 = hash(key);
        int p2 = p1 ^ hash(f);

        return Math.max(this.buckets[p1].get(f), this.buckets[p2].get(f));
    }

    public boolean contains(String key) {

        byte f = fingerprint(key);
        int p1 = hash(key);
        int p2 = p1 ^ hash(f);

        return this.buckets[p1].contains(f) || this.buckets[p2].contains(f);
    }

    private boolean relocateAndInsert(int p1, int p2, byte f, double value) {

        boolean flag = this.random.nextBoolean();
        int replacedBucketPosition = flag ? p1 : p2;

        for (int i = 0; i < MAX_NUM_KICKS; i++) {
            //随机得到被替换bucket中slot的位置
            int replacedSlotPosition = this.random.nextInt(Bucket.BUCKET_SIZE);
            //获取被替换slot中的key和value
            byte replacedF = this.buckets[replacedBucketPosition].slots[replacedSlotPosition].fingerprint;
            double replaceValue = this.buckets[replacedBucketPosition].slots[replacedSlotPosition].value;
            //将f和value插入带替换slot
            this.buckets[replacedBucketPosition].slots[replacedSlotPosition].fingerprint = f;
            this.buckets[replacedBucketPosition].slots[replacedSlotPosition].value = value;

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
        public static int BUCKET_SIZE = 4;

        public Bucket() {
            this.slots = new Slot[BUCKET_SIZE];
            for (int i = 0; i < BUCKET_SIZE; i++) {
                this.slots[i] = new Slot();
            }
        }

        public boolean isFull() {

            for (int i = 0; i < BUCKET_SIZE; i++) {
                if (this.slots[i].flag) {
                    return false;
                }
            }

            return true;
        }

        public boolean add(byte key, double value) {

            for (int i = 0; i < BUCKET_SIZE; i++) {
                if (this.slots[i].flag) {
                    this.slots[i].fingerprint = key;
                    this.slots[i].flag = false;
                    this.slots[i].value = value;
                    return true;
                }
            }

            return false;
        }

        public double get(byte f) {

            for (int i = 0; i < BUCKET_SIZE; i++) {
                if (this.slots[i].fingerprint == f) {
                    return this.slots[i].value;
                }
            }

            return 0.0;
        }

        public boolean contains(byte f) {

            for (int i = 0; i < BUCKET_SIZE; i++) {
                if (this.slots[i].fingerprint == f) {
                    return true;
                }
            }

            return false;
        }
    }

    public static class Slot {

        private byte fingerprint;
        private boolean flag;
        private double value;

        public Slot() {
            this.fingerprint = 0;
            this.flag = true;
            this.value = 0.0;
        }

        public Slot(byte fingerprint, double value) {
            this.fingerprint = fingerprint;
            this.flag = false;
            this.value = value;
        }

        public byte getFingerprint() {
            return this.fingerprint;
        }

        public double getValue() {
            return this.value;
        }
    }

    public byte fingerprint(String key) {

        int h = key.hashCode();

        h += ~(h << 15);
        h ^= (h >> 10);
        h += (h << 3);
        h ^= (h >> 6);
        h += ~(h << 11);
        h ^= (h >> 16);

        return (byte) h;
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
}
