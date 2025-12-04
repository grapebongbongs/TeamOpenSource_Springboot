package com.example.sbb.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 간단하게 여기서 바로 생성해서 사용 (빈으로 주입해도 됨)
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final Map<String, Integer> AVATAR_PRICES = Map.of(
            "🧑", 0,
            "🐱", 10,
            "🐳", 10,
            "🦊", 10,
            "🐯", 12,
            "🐼", 12,
            "👾", 14,
            "🤖", 14
    );

    private static final Map<String, Integer> BANNER_PRICES = Map.of(
            "sunrise", 10,
            "ocean", 15,
            "forest", 15,
            "midnight", 18,
            "aurora", 20
    );

    private static final Map<String, Integer> BOOST_PRICES = Map.of(
            "shield", 20,
            "extra10", 15,
            "extra20", 25
    );

    /**
     * 회원 가입용 유저 생성
     */
    public void createUser(String username, String password, String email) {
        SiteUser user = new SiteUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // 비밀번호 암호화
        user.setEmail(email);
        user.setRole("ROLE_USER"); // Spring Security에서 쓰는 기본 형태

        userRepository.save(user);
    }

    /**
     * username으로 유저 한 명 조회 (로그인 유저 찾을 때 사용)
     */
    public SiteUser getUser(String username) {
        Optional<SiteUser> optionalUser = this.userRepository.findByUsername(username);

        if (optionalUser.isPresent()) {
            return optionalUser.get();
        } else {
            // 없을 때 예외 발생 (나중에 커스텀 예외로 바꿔도 됨)
            throw new RuntimeException("사용자를 찾을 수 없습니다: " + username);
        }
    }

    /**
     * username 중복 체크용 (회원가입 시 사용 가능)
     */
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    /**
     * 문제 풀이 후 포인트/연속 풀이 갱신
     */
    public void recordSolve(SiteUser user, boolean correct) {
        if (user == null) return;
        var today = java.time.LocalDate.now();
        var last = user.getLastSolvedDate();
        if (last == null) {
            user.setStreak(1);
        } else if (last.isEqual(today)) {
            // same day: streak 유지
        } else if (last.plusDays(1).isEqual(today)) {
            user.setStreak(user.getStreak() + 1);
        } else {
            if (user.getShieldItems() > 0) {
                user.setShieldItems(user.getShieldItems() - 1); // 보호 아이템 사용
            } else {
                user.setStreak(1);
            }
        }
        user.setLastSolvedDate(today);

        int base = correct ? 10 : 5;
        int bonus = Math.max(0, user.getStreak());
        user.setPoints(user.getPoints() + base + bonus);
        try {
            userRepository.save(user);
        } catch (Exception ignore) {
            // 포인트 저장 실패 시 로직을 막지 않음
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean updateAvatar(SiteUser user, String avatar) {
        if (user == null || avatar == null) return false;
        if (!AVATAR_PRICES.containsKey(avatar)) return false;
        int price = AVATAR_PRICES.getOrDefault(avatar, 0);
        Set<String> owned = parseOwned(user.getPurchasedAvatars());
        if (user.getAvatar() != null) owned.add(user.getAvatar()); // 현재 장착 중인 아바타는 보유 처리
        boolean alreadyOwned = owned.contains(avatar) || price == 0;
        if (!alreadyOwned && user.getPoints() < price) return false;
        if (!alreadyOwned && price > 0) {
            user.setPoints(user.getPoints() - price);
            owned.add(avatar);
            user.setPurchasedAvatars(String.join(",", owned));
        }
        user.setAvatar(avatar);
        userRepository.save(user);
        return true;
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean updateBanner(SiteUser user, String banner) {
        if (user == null || banner == null) return false;
        if (!BANNER_PRICES.containsKey(banner)) return false;
        int price = BANNER_PRICES.getOrDefault(banner, 0);
        Set<String> owned = parseOwned(user.getPurchasedBanners());
        if (user.getBanner() != null) owned.add(user.getBanner()); // 현재 장착 중인 배너는 보유 처리
        boolean alreadyOwned = owned.contains(banner) || price == 0;
        if (!alreadyOwned && user.getPoints() < price) return false;
        if (!alreadyOwned && price > 0) {
            user.setPoints(user.getPoints() - price);
            owned.add(banner);
            user.setPurchasedBanners(String.join(",", owned));
        }
        user.setBanner(banner);
        userRepository.save(user);
        return true;
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean equipBadge(SiteUser user, String badgeId, Set<String> unlocked) {
        if (user == null || badgeId == null) return false;
        if (unlocked == null || !unlocked.contains(badgeId)) return false;
        user.setActiveBadge(badgeId);
        Set<String> owned = parseOwned(user.getPurchasedBadges());
        owned.add(badgeId);
        user.setPurchasedBadges(String.join(",", owned));
        userRepository.save(user);
        return true;
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean grantBadge(SiteUser user, String badgeId) {
        if (user == null || badgeId == null) return false;
        Set<String> owned = parseOwned(user.getPurchasedBadges());
        if (owned.contains(badgeId)) return false;
        owned.add(badgeId);
        user.setPurchasedBadges(String.join(",", owned));
        userRepository.save(user);
        return true;
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean purchaseBoost(SiteUser user, String boostId) {
        if (user == null || boostId == null) return false;
        Integer price = BOOST_PRICES.get(boostId);
        if (price == null) return false;
        if (user.getPoints() < price) return false;
        user.setPoints(user.getPoints() - price);
        switch (boostId) {
            case "shield" -> user.setShieldItems(user.getShieldItems() + 1);
            case "extra10" -> user.setExtraProblemTokens(user.getExtraProblemTokens() + 10);
            case "extra20" -> user.setExtraProblemTokens(user.getExtraProblemTokens() + 20);
            default -> { return false; }
        }
        userRepository.save(user);
        return true;
    }

    public Set<String> parseOwned(String raw) {
        if (raw == null || raw.isBlank()) return new HashSet<>();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }

    public Map<String, Integer> getAvatarPrices() {
        return AVATAR_PRICES;
    }

    public Map<String, Integer> getBannerPrices() {
        return BANNER_PRICES;
    }

    public Map<String, Integer> getBoostPrices() {
        return BOOST_PRICES;
    }

    @org.springframework.transaction.annotation.Transactional
    public SiteUser save(SiteUser user) {
        return userRepository.save(user);
    }
}
