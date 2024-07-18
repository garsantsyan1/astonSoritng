package sorting;

public class TimSortStrategy implements SortStrategy<Integer> {

    private static final int MIN_MERGE = 32;

    /**
     * Отсортированный массив
     */
    private final Integer[] a;

    /**
     * Определяет минимальное количество элементов, выбранных из одной из половин,
     * которые мы объединяем, для запуска режима галопирования.
     * Используется как начальное значение, действительно используется в коде minGallop.
     */
    private static final int MIN_GALLOP = 7;

    /**
     * Определяет минимальное количество элементов, выбранных из одной из половин,
     * которые мы объединяем, для запуска режима галопирования. Меняется в
     * зависимости от структурированности данных, в методах mergeLo и mergeHi.
     */
    private int minGallop = MIN_GALLOP;

    /**
     * Максимальный начальный размер временного массива tmp, используемого
     * для слияния. Этот массив может расширяться.
     * В отличие от оригинальной версии на C, мы не выделяем столько памяти
     * при сортировке меньших массивов. Это изменение было необходимо для повышения
     * производительности.
     */
    private static final int INITIAL_TMP_STORAGE_LENGTH = 256;

    private Integer[] tmp; // временное хранилище для слияний.
    private int tmpBase; // начало среза
    private int tmpLen;  // длина среза

    /**
     * Стек ожидающих слияния прогонов. Прогон i начинается с адреса
     * runBase[i] и продолжается на runLen[i] элементов. Всегда верно
     * (пока индексы находятся в пределах допустимого диапазона), что:
     *     <p>runBase[i] + runLen[i] == runBase[i + 1]<p>
     * поэтому мы могли бы сократить объем памяти для этого, но это небольшое количество,
     * и хранение всей информации явно упрощает код.
     */
    private int stackSize = 0;  // Количество ожидающих прогонов в стеке
    private final int[] runBase;
    private final int[] runLen;

    private static Integer countSteps = 0;

    public static int getCountSteps() {
        return countSteps;
    }

    private TimSortStrategy(Integer[] a, Integer[] work, int workBase, int workLen) {
        this.a = a;

        // Выделение временного хранилища (которое может быть увеличено при необходимости)
        int len = a.length;
        int tlen = (len < 2 * INITIAL_TMP_STORAGE_LENGTH) ?
                len >>> 1 : INITIAL_TMP_STORAGE_LENGTH;
        if (work == null || workLen < tlen || workBase + tlen > work.length) {
            tmp = new Integer[tlen];
            tmpBase = 0;
            tmpLen = tlen;
            countSteps++;
        } else {
            tmp = work;
            tmpBase = workBase;
            tmpLen = workLen;
        }

        /*
         * Выделение стека прогонов, которые будут объединены (который не может быть расширен).
         * Требования к длине стека описаны в listsort.txt. Версия на C всегда использует
         * одинаковую длину стека (85), но это оказалось слишком неэффективным  при сортировке
         * "средних" массивов (например, 100 элементов) в Java. Поэтому мы используем меньшие
         * (но достаточно большие) длины стека для меньших массивов. "Магические числа" в
         * вычислении ниже должны быть изменены, если MIN_MERGE уменьшено. См. объявление
         * MIN_MERGE выше для дополнительной информации.
         * Максимальное значение 49 позволяет для массива длиной до Integer.MAX_VALUE-4,
         * если массив заполнен в худшем случае увеличения размера стека. Более подробные
         * объяснения приведены в разделе 4:
         * http://envisage-project.eu/wp-content/uploads/2015/02/sorting.pdf
         */
        int stackLen = (len < 120 ? 5 :
                len < 1542 ? 10 :
                        len < 119151 ? 24 : 49);
        runBase = new int[stackLen];
        runLen = new int[stackLen];
    }

    @Override
    public void sort(Integer[] array) {
        countSteps = 0;
        Integer[] work = new Integer[array.length];
        sort(array, 0, array.length, work, 0, work.length);
        System.out.printf("Количество перестановок: %d\n", countSteps);
    }

    /**
     * Сортирует заданный диапазон, используя заданный рабочий массив для временного
     * хранения при возможности. Этот метод предназначен для вызова из публичных методов
     * (в классе Arrays) после выполнения необходимых проверок границ массива и расширения
     * параметров до требуемых форм.
     *
     * @param a массив, который нужно отсортировать
     * @param lo индекс первого элемента, включительно, который нужно отсортировать
     * @param hi индекс последнего элемента, не включая, который нужно отсортировать
     * @param work рабочий массив (срез)
     * @param workBase начало доступного пространства в рабочем массиве
     * @param workLen доступный размер рабочего массива
     * @since 1.8
     */
    private static void sort(Integer[] a, int lo, int hi, Integer[] work, int workBase, int workLen) {
        assert a != null && lo >= 0 && lo <= hi && hi <= a.length;

        int nRemaining = hi - lo;
        if (nRemaining < 2)
            return;  // Массивы размером 0 и 1 всегда отсортированы

        // Если массив мал, выполняем "мини-TimSort" без слияний
        if (nRemaining < MIN_MERGE) {
            int initRunLen = countRunAndMakeAscending(a, lo, hi);
            binarySort(a, lo, hi, lo + initRunLen);
            return;
        }

        /*
         * Проходим по массиву один раз, слева направо, находя естественные прогоны,
         * расширяя короткие естественные прогоны до minRun элементов и объединяя прогоны
         * для поддержания инварианта стека.
         */
        TimSortStrategy ts = new TimSortStrategy(a, work, workBase, workLen);
        int minRun = minRunLength(nRemaining);
        do {
            // Определяем следующий прогон
            int runLen = countRunAndMakeAscending(a, lo, hi);

            // Если прогон короткий, расширяем до min(minRun, nRemaining)
            if (runLen < minRun) {
                int force = nRemaining <= minRun ? nRemaining : minRun;
                binarySort(a, lo, lo + force, lo + runLen);
                runLen = force;
            }

            // Помещаем прогон в стек ожидающих слияния прогонов и, возможно, объединяем
            ts.pushRun(lo, runLen);
            ts.mergeCollapse();

            // Переходим к следующему прогону
            lo += runLen;
            nRemaining -= runLen;
            countSteps++;
        } while (nRemaining != 0);

        // Объединяем все оставшиеся прогоны для завершения сортировки
        assert lo == hi;
        ts.mergeForceCollapse();
        assert ts.stackSize == 1;
    }
    /**
     * Сортирует указанную часть указанного массива с использованием бинарной
     * сортировки вставкой. Это лучший метод для сортировки небольшого количества
     * элементов. Он требует O(n log n) сравнений, но O(n^2) перемещений данных
     * (в худшем случае).<br>
     * Если начальная часть указанного диапазона уже отсортирована,
     * этот метод может воспользоваться этим: метод предполагает, что элементы
     * от индекса {@code lo}, включительно, до {@code start}, исключительно,
     * уже отсортированы.
     * @param a массив, в котором нужно отсортировать диапазон
     * @param lo индекс первого элемента в диапазоне, который нужно отсортировать
     * @param hi индекс после последнего элемента в диапазоне, который нужно отсортировать
     * @param start индекс первого элемента в диапазоне, который еще не известен как
     *        отсортированный ({@code lo <= start <= hi})
     */
    @SuppressWarnings({"fallthrough", "rawtypes", "unchecked"})
    private static void binarySort(Integer[] a, int lo, int hi, int start) {
        assert lo <= start && start <= hi;
        if (start == lo)
            start++;
        for (; start < hi; start++) {
            Integer pivot = a[start];

            // Устанавливаем left (и right) на индекс, куда должен встать a[start] (pivot)
            int left = lo;
            int right = start;
            assert left <= right;
            /*
             * Инварианты:
             *   pivot >= все в [lo, left).
             *   pivot <  все в [right, start).
             */
            while (left < right) {
                int mid = (left + right) >>> 1;
                countSteps++;
                if (pivot < a[mid])
                    right = mid;
                else
                    left = mid + 1;
            }
            assert left == right;

            /*
             * Инварианты все еще выполняются: pivot >= все в [lo, left) и
             * pivot < все в [left, start), поэтому pivot принадлежит left.
             * Обратите внимание, что если есть элементы, равные pivot, left указывает
             * на первый слот после них -- именно поэтому эта сортировка стабильна.
             * Перемещаем элементы, чтобы освободить место для pivot.
             */
            int n = start - left;  // Количество элементов для перемещения
            // Switch - это просто оптимизация для arraycopy в случае по умолчанию
            switch (n) {
                case 2:  a[left + 2] = a[left + 1];
                case 1:  a[left + 1] = a[left];
                    break;
                default: System.arraycopy(a, left, a, left + 1, n);
            }
            a[left] = pivot;
        }
    }

    /**
     * Возвращает длину прогона, начинающегося с указанной позиции в указанном массиве,
     * и, если прогон убывающий, переворачивает его (гарантируя, что прогон всегда
     * возрастающий, когда метод возвращается).
     * Прогон - это самая длинная возрастающая последовательность с:
     *      <p>a[lo] <= a[lo + 1] <= a[lo + 2] <= ... <p/>
     * или самая длинная убывающая последовательность с:
     *      <p>a[lo] >  a[lo + 1] >  a[lo + 2] >  ...<p/>
     * Для его предполагаемого использования в стабильной сортировке слиянием,
     * строгость определения "убывающий" необходима, чтобы вызов мог безопасно
     * перевернуть убывающую последовательность без нарушения стабильности.
     * @param a массив, в котором нужно посчитать прогон и, возможно, перевернуть
     * @param lo индекс первого элемента в прогоне
     * @param hi индекс после последнего элемента, который может быть включен в прогон.
     *        Требуется, чтобы {@code lo < hi}.
     * @return длина прогона, начинающегося с указанной позиции в указанном массиве
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int countRunAndMakeAscending(Integer[] a, int lo, int hi) {
        assert lo < hi;
        int runHi = lo + 1;
        if (runHi == hi) {
            countSteps++;
            return 1;
        }

        // Находим конец прогона и переворачиваем диапазон, если он убывающий
        if (a[runHi++] < a[lo]) { // Убывающий
            while (runHi < hi && a[runHi] < a[runHi - 1]) {
                countSteps++;
                runHi++;
            }
            reverseRange(a, lo, runHi);
        } else { // Возрастающий
            while (runHi < hi && a[runHi] >= a[runHi - 1]) {
                countSteps++;
                runHi++;
            }
        }
        return runHi - lo;
    }

    /**
     * Переворачивает указанный диапазон указанного массива.
     *
     * @param a массив, в котором нужно перевернуть диапазон
     * @param lo индекс первого элемента в диапазоне, который нужно перевернуть
     * @param hi индекс после последнего элемента в диапазоне, который нужно перевернуть
     */
    private static void reverseRange(Integer[] a, int lo, int hi) {
        hi--;
        while (lo < hi) {
            Integer t = a[lo];
            a[lo++] = a[hi];
            a[hi--] = t;
        }
    }

    /**
     * Возвращает минимальную допустимую длину прогона для массива указанной длины.
     * Если прогон короче этого значения, он будет расширен с помощью {@link #binarySort}.<br>
     * Грубо говоря, вычисление выглядит так:<br>
     * Если n < MIN_MERGE, возвращаем n (он слишком мал для сложных действий).<br>
     * Иначе, если n является точной степенью 2, возвращаем MIN_MERGE/2.<br>
     * Иначе возвращаем целое число k, MIN_MERGE/2 <= k <= MIN_MERGE, такое что n/k<br>
     * близко к, но строго меньше, чем точная степень 2.
     * @param n длина массива,
     * @return длина минимального прогона для слияния
     */
    private static int minRunLength(int n) {
        assert n >= 0;
        int r = 0;      // Становится 1, если какие-либо 1 биты сдвинуты
        while (n >= MIN_MERGE) {
            r |= (n & 1);
            n >>= 1;
        }
        return n + r;
    }

    /**
     * Помещает указанный прогон в стек ожидающих слияния прогонов.
     *
     * @param runBase индекс первого элемента в прогоне
     * @param runLen  количество элементов в прогоне
     */
    private void pushRun(int runBase, int runLen) {
        this.runBase[stackSize] = runBase;
        this.runLen[stackSize] = runLen;
        stackSize++;
    }

    /**
     * Проверяет стек прогонов, ожидающих слияния, и объединяет соседние прогоны
     * до тех пор, пока не будут восстановлены инварианты стека:<br>
     *     1. runLen[i - 3] > runLen[i - 2] + runLen[i - 1]<p>
     *     2. runLen[i - 2] > runLen[i - 1]<p>
     * Этот метод вызывается каждый раз, когда новый прогон помещается в стек,
     * поэтому инварианты гарантированно выполняются для i < stackSize на входе в метод.
     */
    private void mergeCollapse() {
        while (stackSize > 1) {
            int n = stackSize - 2;
            if (n > 0 && runLen[n-1] <= runLen[n] + runLen[n+1] ||
                    n > 1 && runLen[n-2] <= runLen[n] + runLen[n-1]) {
                if (runLen[n - 1] < runLen[n + 1])
                    n--;
            } else if (n < 0 || runLen[n] > runLen[n + 1]) {
                break; // Инвариант установлен
            }
            mergeAt(n);
        }
    }

    /**
     * Объединяет все прогоны в стеке до тех пор, пока не останется один.
     * Этот метод вызывается один раз для завершения сортировки.
     */
    private void mergeForceCollapse() {
        while (stackSize > 1) {
            int n = stackSize - 2;
            if (n > 0 && runLen[n - 1] < runLen[n + 1])
                n--;
            mergeAt(n);
        }
    }

    /**
     * Объединяет два прогона в стеке с индексами i и i+1. Прогон i должен быть
     * предпоследним или предпредпоследним прогоном в стеке. Другими словами,
     * i должно быть равно stackSize-2 или stackSize-3.
     * @param i индекс стека первого из двух прогонов для слияния
     */
    @SuppressWarnings("unchecked")
    private void mergeAt(int i) {
        assert stackSize >= 2;
        assert i >= 0;
        assert i == stackSize - 2 || i == stackSize - 3;

        int base1 = runBase[i];
        int len1 = runLen[i];
        int base2 = runBase[i + 1];
        int len2 = runLen[i + 1];
        assert len1 > 0 && len2 > 0;
        assert base1 + len1 == base2;

        /*
         * Записываем длину объединенных прогонов; если i является третьим-последним
         * прогоном, также сдвигаем последний прогон (который не участвует
         * в этом слиянии). Текущий прогон (i+1) исчезает в любом случае.
         */
        runLen[i] = len1 + len2;
        if (i == stackSize - 3) {
            runBase[i + 1] = runBase[i + 2];
            runLen[i + 1] = runLen[i + 2];
        }
        stackSize--;

        /*
         * Находим, где первый элемент прогона 2 входит в прогон 1. Предыдущие элементы
         * в прогоне 1 можно игнорировать (потому что они уже на своих местах).
         */
        int k = gallopRight(a[base2], a, base1, len1, 0);
        assert k >= 0;
        base1 += k;
        len1 -= k;
        if (len1 == 0)
            return;

        /*
         * Находим, где последний элемент прогона 1 входит в прогон 2. Последующие элементы
         * в прогоне 2 можно игнорировать (потому что они уже на своих местах).
         */
        len2 = gallopLeft(a[base1 + len1 - 1], a,
                base2, len2, len2 - 1);
        assert len2 >= 0;
        if (len2 == 0)
            return;

        // Объединяем оставшиеся прогоны, используя временный массив с min(len1, len2) элементами
        if (len1 <= len2)
            mergeLo(base1, len1, base2, len2);
        else
            mergeHi(base1, len1, base2, len2);
    }

    /**
     * Находит позицию, в которую нужно вставить указанный ключ в указанный
     * отсортированный диапазон; если диапазон содержит элемент, равный ключу,
     * возвращает индекс самого левого равного элемента.
     *
     * @param key ключ, позицию которого нужно найти
     * @param a массив, в котором нужно искать
     * @param base индекс первого элемента в диапазоне
     * @param len длина диапазона; должна быть > 0
     * @param hint индекс, с которого нужно начинать поиск, 0 <= hint < n.
     *     Чем ближе hint к результату, тем быстрее будет работать метод.
     * @return целое число k,  0 <= k <= n такое, что a[b + k - 1] < key <= a[b + k],
     *    при этом a[b - 1] считается минус бесконечностью, а a[b + n] - плюс бесконечностью.
     *    Другими словами, ключ должен находиться по индексу b + k; или в других словах,
     *    первые k элементов a должны предшествовать ключу, а последние n - k
     *    должны следовать за ним.
     */
    private static int gallopLeft(Integer key, Integer[] a,
                                  int base, int len, int hint) {
        assert len > 0 && hint >= 0 && hint < len;

        int lastOfs = 0;
        int ofs = 1;
        if (key > a[base + hint]) {
            // Галопируем вправо, пока a[base+hint+lastOfs] < key <= a[base+hint+ofs]
            int maxOfs = len - hint;
            while (ofs < maxOfs && key > a[base + hint + ofs]) {
                countSteps++;
                lastOfs = ofs;
                ofs = (ofs << 1) + 1;
                if (ofs <= 0)   // int overflow
                    ofs = maxOfs;
            }
            if (ofs > maxOfs)
                ofs = maxOfs;

            // Делаем смещения относительно base
            lastOfs += hint;
            ofs += hint;
        } else { // key <= a[base + hint]
            // Галопируем влево, пока a[base+hint-ofs] < key <= a[base+hint-lastOfs]
            final int maxOfs = hint + 1;
            while (ofs < maxOfs && key <= a[base + hint - ofs]) {
                countSteps++;
                lastOfs = ofs;
                ofs = (ofs << 1) + 1;
                if (ofs <= 0)   // int overflow
                    ofs = maxOfs;
            }
            if (ofs > maxOfs)
                ofs = maxOfs;

            // Делаем смещения относительно base
            int tmp = lastOfs;
            lastOfs = hint - ofs;
            ofs = hint - tmp;
        }
        assert -1 <= lastOfs && lastOfs < ofs && ofs <= len;

        /*
         * Теперь a[base+lastOfs] < key <= a[base+ofs], поэтому ключ принадлежит
         * где-то справа от lastOfs, но не дальше ofs. Делаем бинарный поиск,
         * с инвариантом a[base + lastOfs - 1] < key <= a[base + ofs].
         */
        lastOfs++;
        while (lastOfs < ofs) {
            countSteps++;
            int m = lastOfs + ((ofs - lastOfs) >>> 1);

            if (key > a[base + m])
                lastOfs = m + 1;  // a[base + m] < key
            else
                ofs = m;          // key <= a[base + m]
        }
        assert lastOfs == ofs;    // поэтому a[base + ofs - 1] < key <= a[base + ofs]
        return ofs;
    }

    /**
     * Подобно gallopLeft, за исключением того, что если диапазон содержит элемент,
     * равный ключу, gallopRight возвращает индекс после самого правого равного элемента.
     *
     * @param key ключ, позицию которого нужно найти
     * @param a массив, в котором нужно искать
     * @param base индекс первого элемента в диапазоне
     * @param len длина диапазона; должна быть > 0
     * @param hint индекс, с которого нужно начинать поиск, 0 <= hint < n.
     *     Чем ближе hint к результату, тем быстрее будет работать метод.
     * @return целое число k,  0 <= k <= n такое, что a[b + k - 1] <= key < a[b + k]
     */
    private static int gallopRight(Integer key, Integer[] a,
                                   int base, int len, int hint) {
        assert len > 0 && hint >= 0 && hint < len;

        int ofs = 1;
        int lastOfs = 0;
        if (key < a[base + hint]) {
            // Галопируем влево, пока a[b+hint - ofs] <= key < a[b+hint - lastOfs]
            int maxOfs = hint + 1;
            while (ofs < maxOfs && key < a[base + hint - ofs]) {
                countSteps++;
                lastOfs = ofs;
                ofs = (ofs << 1) + 1;
                if (ofs <= 0)   // int overflow
                    ofs = maxOfs;
            }
            if (ofs > maxOfs)
                ofs = maxOfs;

            // Делаем смещения относительно b
            int tmp = lastOfs;
            lastOfs = hint - ofs;
            ofs = hint - tmp;
        } else { // a[b + hint] <= key
            // Галопируем вправо, пока a[b+hint + lastOfs] <= key < a[b+hint + ofs]
            int maxOfs = len - hint;
            while (ofs < maxOfs && key >= a[base + hint + ofs]) {
                countSteps++;
                lastOfs = ofs;
                ofs = (ofs << 1) + 1;
                if (ofs <= 0)   // int overflow
                    ofs = maxOfs;
            }
            if (ofs > maxOfs)
                ofs = maxOfs;

            // Делаем смещения относительно b
            lastOfs += hint;
            ofs += hint;
        }
        assert -1 <= lastOfs && lastOfs < ofs && ofs <= len;

        /*
         * Теперь a[b + lastOfs] <= key < a[b + ofs], поэтому ключ принадлежит
         * где-то справа от lastOfs, но не дальше ofs. Делаем бинарный поиск,
         * с инвариантом a[b + lastOfs - 1] <= key < a[b + ofs].
         */
        lastOfs++;
        while (lastOfs < ofs) {
            countSteps++;
            int m = lastOfs + ((ofs - lastOfs) >>> 1);

            if (key < a[base + m])
                ofs = m;          // key < a[b + m]
            else
                lastOfs = m + 1;  // a[b + m] <= key
        }
        assert lastOfs == ofs;    // поэтому a[b + ofs - 1] <= key < a[b + ofs]
        return ofs;
    }

/**
 * Объединяет два соседних прогона в месте, (стабильно).<br>
 * Первый элемент первого прогона должен быть больше первого элемента
 * второго прогона (a[base1] > a[base2]), и последний элемент первого прогона
 * (a[base1 + len1-1]) должен быть больше всех элементов второго прогона.<br>
 * Для производительности этот метод должен вызываться только если len1 <= len2;
 * его аналог, mergeHi, должен вызываться если len1 >= len2. (Любой метод может быть вызван,
 * если len1 == len2.)
 * @param base1 индекс первого элемента в первом прогоне для слияния
 * @param len1  длина первого прогона для слияния (должна быть > 0)
 * @param base2 индекс первого элемента во втором прогоне для слияния
 *        (должен быть aBase + aLen)
 * @param len2  длина второго прогона для слияния (должна быть > 0)
 */
@SuppressWarnings({"unchecked", "rawtypes"})
private void mergeLo(int base1, int len1, int base2, int len2) {
    assert len1 > 0 && len2 > 0 && base1 + len1 == base2;

    // Копируем первый прогон во временный массив
    Integer[] a = this.a; // Для производительности
    Integer[] tmp = (Integer[]) ensureCapacity(len1);

    int cursor1 = tmpBase; // Индексы во временном массиве
    int cursor2 = base2;   // Индексы в a
    int dest = base1;      // Индексы в a
    System.arraycopy(a, base1, tmp, cursor1, len1);

    // Перемещаем первый элемент второго прогона и обрабатываем вырожденные случаи
    a[dest++] = a[cursor2++];
    if (--len2 == 0) {
        System.arraycopy(tmp, cursor1, a, dest, len1);
        return;
    }
    if (len1 == 1) {
        System.arraycopy(a, cursor2, a, dest, len2);
        a[dest + len2] = tmp[cursor1]; // Последний элемент первого прогона в конец слияния
        return;
    }

    int minGallop = this.minGallop;  // Используем локальную переменную для производительности
    outer:
    while (true) {
        int count1 = 0; // Количество раз подряд, когда первый прогон победил
        int count2 = 0; // Количество раз подряд, когда второй прогон победил

        /*
         * Делаем прямолинейное слияние до тех пор, пока (если когда-либо) один из прогонов
         * не начнет побеждать стабильно.
         */
        do {
            assert len1 > 1 && len2 > 0;
            countSteps++;
            if (a[cursor2] < tmp[cursor1]) {
                a[dest++] = a[cursor2++];
                count2++;
                count1 = 0;
                if (--len2 == 0)
                    break outer;
            } else {
                a[dest++] = tmp[cursor1++];
                count1++;
                count2 = 0;
                if (--len1 == 1)
                    break outer;
            }
        } while ((count1 | count2) < minGallop);

        /*
         * Один прогон побеждает так стабильно, что галопирование может быть огромным
         * выигрышем. Поэтому пробуем это, и продолжаем галопирование до тех пор, пока (если когда-либо)
         * ни один из прогонов не начнет побеждать стабильно.
         */
        do {
            countSteps++;
            assert len1 > 1 && len2 > 0;
            count1 = gallopRight(a[cursor2], tmp, cursor1, len1, 0);
            if (count1 != 0) {
                System.arraycopy(tmp, cursor1, a, dest, count1);
                dest += count1;
                cursor1 += count1;
                len1 -= count1;
                if (len1 <= 1)  // len1 == 1 || len1 == 0
                    break outer;
            }
            a[dest++] = a[cursor2++];
            if (--len2 == 0)
                break outer;

            count2 = gallopLeft(tmp[cursor1], a, cursor2, len2, 0);
            if (count2 != 0) {
                System.arraycopy(a, cursor2, a, dest, count2);
                dest += count2;
                cursor2 += count2;
                len2 -= count2;
                if (len2 == 0)
                    break outer;
            }
            a[dest++] = tmp[cursor1++];
            if (--len1 == 1)
                break outer;
            minGallop--;
        } while (count1 >= MIN_GALLOP | count2 >= MIN_GALLOP);
        if (minGallop < 0)
            minGallop = 0;
        minGallop += 2;  // Штрафуем за выход из режима галопирования
    }  // Конец "outer" цикла
    this.minGallop = minGallop < 1 ? 1 : minGallop;  // Записываем обратно в поле

    if (len1 == 1) {
        assert len2 > 0;
        System.arraycopy(a, cursor2, a, dest, len2);
        a[dest + len2] = tmp[cursor1]; // Последний элемент первого прогона в конец слияния
    } else if (len1 == 0) {
        throw new IllegalArgumentException(
                "Comparison method violates its general contract!");
    } else {
        assert len2 == 0;
        assert len1 > 1;
        System.arraycopy(tmp, cursor1, a, dest, len1);
    }
}

    /**
     * Подобно mergeLo, за исключением того, что этот метод должен вызываться только если
     * len1 >= len2; mergeLo должен вызываться если len1 <= len2. (Любой метод может быть вызван,
     * если len1 == len2.)
     *
     * @param base1 индекс первого элемента в первом прогоне для слияния
     * @param len1  длина первого прогона для слияния (должна быть > 0)
     * @param base2 индекс первого элемента во втором прогоне для слияния
     *        (должен быть aBase + aLen)
     * @param len2  длина второго прогона для слияния (должна быть > 0)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mergeHi(int base1, int len1, int base2, int len2) {
        assert len1 > 0 && len2 > 0 && base1 + len1 == base2;

        // Копируем второй прогон во временный массив
        Integer[] a = this.a; // Для производительности
        Integer[] tmp = (Integer[]) ensureCapacity(len2);
        int tmpBase = this.tmpBase;
        System.arraycopy(a, base2, tmp, tmpBase, len2);

        int cursor1 = base1 + len1 - 1;  // Индексы в a
        int cursor2 = tmpBase + len2 - 1; // Индексы во временном массиве
        int dest = base2 + len2 - 1;     // Индексы в a

        // Перемещаем последний элемент первого прогона и обрабатываем вырожденные случаи
        a[dest--] = a[cursor1--];
        if (--len1 == 0) {
            System.arraycopy(tmp, tmpBase, a, dest - (len2 - 1), len2);
            return;
        }
        if (len2 == 1) {
            dest -= len1;
            cursor1 -= len1;
            System.arraycopy(a, cursor1 + 1, a, dest + 1, len1);
            a[dest] = tmp[cursor2];
            return;
        }

        int minGallop = this.minGallop;  // Используем локальную переменную для производительности
        outer:
        while (true) {
            int count1 = 0; // Количество раз подряд, когда первый прогон победил
            int count2 = 0; // Количество раз подряд, когда второй прогон победил

            /*
             * Делаем прямолинейное слияние до тех пор, пока (если когда-либо) один из прогонов
             * не начнет побеждать стабильно.
             */
            do {
                assert len1 > 0 && len2 > 1;
                countSteps++;
                if (tmp[cursor2] < a[cursor1]) {
                    a[dest--] = a[cursor1--];
                    count1++;
                    count2 = 0;
                    if (--len1 == 0)
                        break outer;
                } else {
                    a[dest--] = tmp[cursor2--];
                    count2++;
                    count1 = 0;
                    if (--len2 == 1)
                        break outer;
                }
            } while ((count1 | count2) < minGallop);

            /*
             * Один прогон побеждает так стабильно, что галопирование может быть огромным
             * выигрышем. Поэтому пробуем это, и продолжаем галопирование до тех пор, пока (если когда-либо)
             * ни один из прогонов не начнет побеждать стабильно.
             */
            do {
                assert len1 > 0 && len2 > 1;
                countSteps++;
                count1 = len1 - gallopRight(tmp[cursor2], a, base1, len1, len1 - 1);
                if (count1 != 0) {
                    dest -= count1;
                    cursor1 -= count1;
                    len1 -= count1;
                    System.arraycopy(a, cursor1 + 1, a, dest + 1, count1);
                    if (len1 == 0)
                        break outer;
                }
                a[dest--] = tmp[cursor2--];
                if (--len2 == 1)
                    break outer;

                count2 = len2 - gallopLeft(a[cursor1], tmp, tmpBase, len2, len2 - 1);
                if (count2 != 0) {
                    dest -= count2;
                    cursor2 -= count2;
                    len2 -= count2;
                    System.arraycopy(tmp, cursor2 + 1, a, dest + 1, count2);
                    if (len2 <= 1)
                        break outer; // len2 == 1 || len2 == 0
                }
                a[dest--] = a[cursor1--];
                if (--len1 == 0)
                    break outer;
                minGallop--;
            } while (count1 >= MIN_GALLOP | count2 >= MIN_GALLOP);
            if (minGallop < 0)
                minGallop = 0;
            minGallop += 2;  // Штрафуем за выход из режима галопирования
        }  // Конец "outer" цикла
        this.minGallop = minGallop < 1 ? 1 : minGallop;  // Записываем обратно в поле

        if (len2 == 1) {
            assert len1 > 0;
            dest -= len1;
            cursor1 -= len1;
            System.arraycopy(a, cursor1 + 1, a, dest + 1, len1);
            a[dest] = tmp[cursor2];  // Перемещаем первый элемент второго прогона в начало слияния
        } else if (len2 == 0) {
            throw new IllegalArgumentException(
                    "Comparison method violates its general contract!");
        } else {
            assert len1 == 0;
            assert len2 > 0;
            System.arraycopy(tmp, tmpBase, a, dest - (len2 - 1), len2);
        }
    }

    /**
     * Обеспечивает, что внешний массив tmp имеет как минимум указанное количество
     * элементов, увеличивая его размер при необходимости. Размер увеличивается
     * экспоненциально для обеспечения амортизированной линейной временной сложности.
     * @param minCapacity минимальная требуемая емкость временного массива
     * @return tmp, независимо от того, увеличился он или нет
     */
    private Integer[] ensureCapacity(int minCapacity) {
        if (tmpLen < minCapacity) {
            // Вычисляем наименьшую степень 2 > minCapacity
            int newSize = -1 >>> Integer.numberOfLeadingZeros(minCapacity);
            newSize++;

            if (newSize < 0) // Невероятно!
                newSize = minCapacity;
            else
                newSize = Math.min(newSize, a.length >>> 1);

            @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
            Integer[] newArray = new Integer[newSize];
            tmp = newArray;
            tmpLen = newSize;
            tmpBase = 0;
        }
        return tmp;
    }
}