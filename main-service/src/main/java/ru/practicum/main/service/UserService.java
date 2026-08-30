package ru.practicum.main.service;

import ru.practicum.main.dto.NewUserRequest;
import ru.practicum.main.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto createUser(NewUserRequest request);
    List<UserDto> getUsers(List<Long> ids, int from, int size);
    void deleteUser(Long userId);
}