package com.example.staffapp.mapper;

import com.example.staffapp.model.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface ItemAPIMapper {
    /**
     * Karena beda microservice, kita hanya bisa read only dari tabel inventory.
     * (Tidak ada Insert/update/delete)
     */
    final String getAll = "SELECT * FROM inventory";
    final String getById = "SELECT * FROM inventory WHERE idItem = #{idItem}";

    @Select(getAll)
    List<Item> getAll();

    @Select(getById)
    @Results(value = {
            @Result(property = "idItem", column = "idItem"),
            @Result(property = "itemType", column = "itemType"),
            @Result(property = "available", column = "available")
    })
    Item getById(long idItem);
}
