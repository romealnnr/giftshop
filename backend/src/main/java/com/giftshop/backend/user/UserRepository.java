package com.giftshop.backend.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepository(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

       /**
     * Saves a new user.
     * Uniqueness of the email is enforced by the database UNIQUE constraint.
     * The email must already be normalized (trimmed and lowercase).
     *
     * @throws EmailAlreadyExistsException if the email is already registered
     */
    public void save(User user, String passwordHash) {
        try {
            jpaRepository.saveAndFlush(UserEntity.from(user, passwordHash));
        } catch (DataIntegrityViolationException e) {
            if (jpaRepository.existsByEmail(user.email())) {
                throw new EmailAlreadyExistsException(e);
            }
            throw e;
        }
    }

    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(UserEntity::toUser);
    }

    public Optional<UserCredentials> findCredentialsByEmail(String email) {
        return jpaRepository.findByEmail(email).map(UserEntity::toCredentials);
    }
}