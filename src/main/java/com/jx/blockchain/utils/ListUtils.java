package com.jx.blockchain.utils;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;


public class ListUtils {
    @SuppressWarnings("unchecked")
    public static <E> List<E> of() {
        return Collections.emptyList();
    }

    public static <E> List<E> of(E e1) {
        List<E> list = new ArrayList<>(1);
        list.add(e1);
        return list;
    }

    public static <E> List<E> of(E e1, E e2) {
        List<E> list = new ArrayList<>(2);
        list.add(e1);
        list.add(e2);
        return list;
    }

    public static <E> List<E> of(E e1, E e2, E e3) {
        List<E> list = new ArrayList<>(3);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        return list;
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4) {
        List<E> list = new ArrayList<>(4);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        return list;
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5) {
        List<E> list = new ArrayList<>(5);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        return list;
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        List<E> list = new ArrayList<>(6);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        list.add(e6);
        return list;
    }


    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        List<E> list = new ArrayList<>(7);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        list.add(e6);
        list.add(e7);
        return list;
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        List<E> list = new ArrayList<>(8);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        list.add(e6);
        list.add(e7);
        list.add(e8);
        return list;
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        List<E> list = new ArrayList<>(9);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        list.add(e6);
        list.add(e7);
        list.add(e8);
        list.add(e9);
        return list;
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        List<E> list = new ArrayList<>(10);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        list.add(e6);
        list.add(e7);
        list.add(e8);
        list.add(e10);
        return list;
    }




    /**
     * 新建一个空List
     *
     * @param <T>      集合元素类型
     * @param isLinked 是否新建LinkedList
     * @return List对象
     */
    public static <T> List<T> list(boolean isLinked) {
        return isLinked ? new LinkedList<>() : new ArrayList<>();
    }


    /**
     * 新建一个List
     *
     * @param <T>        集合元素类型
     * @param isLinked   是否新建LinkedList
     * @param collection 集合
     * @return List对象
     */
    public static <T> List<T> list(boolean isLinked, Collection<T> collection) {
        if (null == collection) {
            return list(isLinked);
        }
        return isLinked ? new LinkedList<>(collection) : new ArrayList<>(collection);
    }

    /**
     * 新建一个List<br>
     * 提供的参数为null时返回空{@link ArrayList}
     *
     * @param <T>      集合元素类型
     * @param isLinked 是否新建LinkedList
     * @param iterable {@link Iterable}
     * @return List对象
     */
    public static <T> List<T> list(boolean isLinked, Iterable<T> iterable) {
        if (null == iterable) {
            return list(isLinked);
        }
        return list(isLinked, iterable.iterator());
    }

    /**
     * 新建一个List<br>
     * 提供的参数为null时返回空{@link ArrayList}
     *
     * @param <T>      集合元素类型
     * @param isLinked 是否新建LinkedList
     * @param iter     {@link Iterator}
     * @return ArrayList对象
     */
    public static <T> List<T> list(boolean isLinked, Iterator<T> iter) {
        final List<T> list = list(isLinked);
        if (null != iter) {
            while (iter.hasNext()) {
                list.add(iter.next());
            }
        }
        return list;
    }

    /**
     * 新建一个List<br>
     * 提供的参数为null时返回空{@link ArrayList}
     *
     * @param <T>         集合元素类型
     * @param isLinked    是否新建LinkedList
     * @param enumeration {@link Enumeration}
     * @return ArrayList对象
     */
    public static <T> List<T> list(boolean isLinked, Enumeration<T> enumeration) {
        final List<T> list = list(isLinked);
        if (null != enumeration) {
            while (enumeration.hasMoreElements()) {
                list.add(enumeration.nextElement());
            }
        }
        return list;
    }

    /**
     * 新建一个CopyOnWriteArrayList
     *
     * @param <T>        集合元素类型
     * @param collection 集合
     * @return {@link CopyOnWriteArrayList}
     */
    public static <T> CopyOnWriteArrayList<T> toCopyOnWriteArrayList(Collection<T> collection) {
        return (null == collection) ? (new CopyOnWriteArrayList<>()) : (new CopyOnWriteArrayList<>(collection));
    }

    /**
     * 新建一个ArrayList
     *
     * @param <T>        集合元素类型
     * @param collection 集合
     * @return ArrayList对象
     */
    public static <T> ArrayList<T> toList(Collection<T> collection) {
        return (ArrayList<T>) list(false, collection);
    }

    /**
     * 新建一个ArrayList<br>
     * 提供的参数为null时返回空{@link ArrayList}
     *
     * @param <T>      集合元素类型
     * @param iterable {@link Iterable}
     * @return ArrayList对象
     */
    public static <T> ArrayList<T> toList(Iterable<T> iterable) {
        return (ArrayList<T>) list(false, iterable);
    }

    /**
     * 新建一个ArrayList<br>
     * 提供的参数为null时返回空{@link ArrayList}
     *
     * @param <T>      集合元素类型
     * @param iterator {@link Iterator}
     * @return ArrayList对象
     */
    public static <T> ArrayList<T> toList(Iterator<T> iterator) {
        return (ArrayList<T>) list(false, iterator);
    }

    /**
     * 新建一个ArrayList<br>
     * 提供的参数为null时返回空{@link ArrayList}
     *
     * @param <T>         集合元素类型
     * @param enumeration {@link Enumeration}
     * @return ArrayList对象
     */
    public static <T> ArrayList<T> toList(Enumeration<T> enumeration) {
        return (ArrayList<T>) list(false, enumeration);
    }





    /**
     * 反序给定List，会在原List基础上直接修改
     *
     * @param <T>  元素类型
     * @param list 被反转的List
     * @return 反转后的List
     */
    public static <T> List<T> reverse(List<T> list) {
        Collections.reverse(list);
        return list;
    }


    /**
     * 设置或增加元素。当index小于List的长度时，替换指定位置的值，否则在尾部追加
     *
     * @param <T>     元素类型
     * @param list    List列表
     * @param index   位置
     * @param element 新元素
     * @return 原List
     */
    public static <T> List<T> setOrAppend(List<T> list, int index, T element) {
        if (index < list.size()) {
            list.set(index, element);
        } else {
            list.add(element);
        }
        return list;
    }

    /**
     * 截取集合的部分
     *
     * @param <T>   集合元素类型
     * @param list  被截取的数组
     * @param start 开始位置（包含）
     * @param end   结束位置（不包含）
     * @return 截取后的数组，当开始位置超过最大时，返回空的List
     */
    public static <T> List<T> sub(List<T> list, int start, int end) {
        return sub(list, start, end, 1);
    }

    /**
     * 截取集合的部分<br>
     * 此方法与{@link List#subList(int, int)} 不同在于子列表是新的副本，操作子列表不会影响原列表。
     *
     * @param <T>   集合元素类型
     * @param list  被截取的数组
     * @param start 开始位置（包含）
     * @param end   结束位置（不包含）
     * @param step  步进
     * @return 截取后的数组，当开始位置超过最大时，返回空的List
     */
    public static <T> List<T> sub(List<T> list, int start, int end, int step) {
        if (list == null) {
            return null;
        }

        if (list.isEmpty()) {
            return new ArrayList<>(0);
        }

        final int size = list.size();
        if (start < 0) {
            start += size;
        }
        if (end < 0) {
            end += size;
        }
        if (start == size) {
            return new ArrayList<>(0);
        }
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }
        if (end > size) {
            if (start >= size) {
                return new ArrayList<>(0);
            }
            end = size;
        }

        if (step < 1) {
            step = 1;
        }

        final List<T> result = new ArrayList<>();
        for (int i = start; i < end; i += step) {
            result.add(list.get(i));
        }
        return result;
    }



    /**
     * 将对应List转换为不可修改的List
     *
     * @param list List
     * @param <T>  元素类型
     * @return 不可修改List
     */
    public static <T> List<T> unmodifiable(List<T> list) {
        if (null == list) {
            return null;
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * 获取一个空List，这个空List不可变
     *
     * @param <T> 元素类型
     * @return 空的List
     * @see Collections#emptyList()
     */
    public static <T> List<T> empty() {
        return Collections.emptyList();
    }




    // 函数式编程
    public static <T, R> List<R> map(Collection<T> collection, Function<T, R> mapper) {
        if (collection == null || collection.size() == 0) return Collections.emptyList();
        List<R> result = new ArrayList<>(collection.size());
        for (T element : collection) {
            result.add(mapper.apply(element));
        }
        return result;
    }

    public static <T> List<T> filter(Collection<T> collection, Predicate<? super T> predicate) {
        if (collection == null) return null;
        List<T> result = new ArrayList<>(1 + collection.size() / 2);
        for (T element : collection) {
            if (predicate.test(element)) result.add(element);
        }
        return result;
    }

    public static <T> int indexOf(List<T> collection, Predicate<? super T> predicate) {
        if (collection == null) return -1;
        for (int i = 0; i < collection.size(); i++) {
            T element = collection.get(i);
            if (predicate.test(element)) return i;
        }
        return -1;
    }

    public static <T> int lastIndexOf(List<T> collection, Predicate<? super T> predicate) {
        if (collection == null) return -1;
        for (int i = collection.size() - 1; i >= 0; i--) {
            T element = collection.get(i);
            if (predicate.test(element)) return i;
        }
        return -1;
    }

    public static <T, R> List<T> distinctBy(List<T> collection, Function<T, R> mapper) {
        if (collection == null) return null;
        List<T> result = new ArrayList<>(collection.size());
        Set<R> seenSet = new HashSet<>(collection.size());
        for (T element : collection) {
            R apply = mapper.apply(element);
            if (seenSet.add(apply)) result.add(element);
        }
        return result;
    }

    public static <K, V> List<K> project(List<V> list, Function<V, K> keyGetter) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        List<K> kList = new ArrayList<>(list.size());
        for (V val : list) {
            K key = keyGetter.apply(val);
            if (key != null) {
                kList.add(key);
            }
        }
        return kList;
    }
}
