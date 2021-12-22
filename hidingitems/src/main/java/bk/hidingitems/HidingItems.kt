package bk.hidingitems

/**
 * @author Bizyur Konstantin <bkonst2180@gmail.com>
 * @since 21.12.2021
 *
 * Интерфейс поддержки скрытия/показа элементов
 */
interface HidingItems {

    /**
     * Скрыть элементы
     *
     * @param fromIndex Индекс первого элемента
     * @param count Количество элементов
     */
    fun hide(fromIndex: Int, count: Int)

    /**
     * Проверить, является ли элемент скрытым
     *
     * Для проверки получаем позицию элемента по индексу и сравниваем со значеним HIDDEN_ITEM
     *
     * @param index Индекс элемента
     * @return True если элемент скрыт
     */
    fun isHidden(index: Int): Boolean

    /**
     * Показать элементы
     *
     * @param fromIndex Индекс первого элемента
     * @param count Количество элементов
     */
    fun show(fromIndex: Int, count: Int)

}
