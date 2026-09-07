package com.portfolio.repository;
import com.portfolio.domain.AppUser; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface AppUserRepository extends JpaRepository<AppUser,String> { Optional<AppUser> findByUserId(String userId); }
