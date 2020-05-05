package com.example.staffapp.mapper;

import com.example.staffapp.model.Staff;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface StaffMapper {
    final String getAll = "SELECT * FROM staffs";
    final String getById = "SELECT * FROM staffs WHERE idStaff = #{idStaff}";
    final String deleteById = "DELETE from staffs WHERE idStaff = #{idStaff}";
    final String insert = "INSERT INTO staffs (name, division) VALUES (#{name}, #{division})";
    final String update = "UPDATE staffs SET name = #{name}, division = #{division} WHERE idStaff = #{idStaff}";

    @Select(getAll)
    List<Staff> getAll();

    @Select(getById)
    @Results(value = {
            @Result(property = "idStaff", column = "idStaff"),
            @Result(property = "name", column = "name"),
            @Result(property = "division", column = "division")
    })
    Staff getById(long idStaff);

    @Update(update)
    void update(Staff staff);

    @Delete(deleteById)
    void delete(long idStaff);

    @Insert(insert)
    @Options(useGeneratedKeys = true, keyProperty = "idStaff")
    void insert(Staff staff);
}
