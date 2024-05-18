package net.william278.huskclaims.performance;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.IntStream;

@DisplayName("Performance Tests")
public class PerformanceTests {

    @Test
    @DisplayName("Test Claim lookup performance")
    public void testClaimLookupPerformance() {
        final int amount = 3000;
        final int search = 1000;
        final Random random = new Random();

        final int userAmount = random.nextInt(333) + 1;
        System.out.println("Starting performance test with " + amount + " claims and " + userAmount + " users");
        System.out.println("Ram usage: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + "MB");
        System.out.println("Max ram: " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB");
        final List<UUID> uuids = IntStream.range(0, userAmount).mapToObj(i -> UUID.randomUUID()).toList();

        final Set<Claim> claims = Sets.newHashSetWithExpectedSize(amount);
        for (int i = 0; i < amount; i++) {
            final UUID randomOwner = uuids.get(random.nextInt(userAmount));
            final Region region = getRegionWithMaxArea();
            claims.add(Claim.creteEmptyClaim(randomOwner, region));
        }

        long start = System.currentTimeMillis();
        final ClaimWorld claimWorld = ClaimWorld.convert(0, claims, Maps.newHashMap(), Sets.newHashSet());
        long end = System.currentTimeMillis();

        System.out.println("Loaded " + amount + " claims in " + (end - start) + "ms");

        start = System.currentTimeMillis();
        final OldClaimWorld oldClaimWorld = OldClaimWorld.convert(0, claims, Maps.newHashMap(), Sets.newHashSet());
        end = System.currentTimeMillis();

        System.out.println("Loaded with old system " + amount + " claims in " + (end - start) + "ms");

        final List<Claim> claimsList = new ArrayList<>(claims);

        final List<Region.Point> randomPoints = IntStream.range(0, search).mapToObj(i -> getRandPoint()).toList();

        start = System.currentTimeMillis();
        //find claims at random points
        int found = 0;
        for (int i = 0; i < search; i++) {
            final Region.Point point = randomPoints.get(i);
            final Optional<Claim> claim = claimWorld.getClaimAt(point);
            if (claim.isPresent()) {
                found++;
            }
        }

        end = System.currentTimeMillis();
        System.out.println("Found " + found + " claims in " + (end - start) + "ms");

        start = System.currentTimeMillis();
        //find claims at random points
        found = 0;
        for (int i = 0; i < search; i++) {
            final Region.Point point = randomPoints.get(i);
            final Optional<Claim> claim = oldClaimWorld.getClaimAt(point);
            if (claim.isPresent()) {
                found++;
            }
        }

        end = System.currentTimeMillis();
        System.out.println("Found with old system " + found + " claims in " + (end - start) + "ms");

        //find claims with random owners
        found = 0;
        start = System.currentTimeMillis();
        for (int i = 0; i < 333; i++) {
            final UUID randomOwner = uuids.get(random.nextInt(userAmount));
            final List<Claim> claimsFound = claimWorld.getClaimsByUser(randomOwner);
            if (!claimsFound.isEmpty()) {
                found++;
            }
        }

        end = System.currentTimeMillis();
        System.out.println("Found " + found + " claims by user in " + (end - start) + "ms");

        found = 0;
        start = System.currentTimeMillis();
        for (int i = 0; i < 333; i++) {
            final UUID randomOwner = uuids.get(random.nextInt(userAmount));
            final List<Claim> claimsFound = oldClaimWorld.getClaimsByUser(randomOwner);
            if (!claimsFound.isEmpty()) {
                found++;
            }
        }

        end = System.currentTimeMillis();
        System.out.println("Found with old system " + found + " claims by user in " + (end - start) + "ms");

        start = System.currentTimeMillis();
        // remove claims
        for (int i = 0; i < amount; i++) {
            final Claim claim = claimsList.get(i);
            claimWorld.removeClaim(claim);
        }
        end = System.currentTimeMillis();
        System.out.println("Removed " + amount + " claims in " + (end - start) + "ms");

        start = System.currentTimeMillis();
        // remove claims
        for (int i = 0; i < amount; i++) {
            final Claim claim = claimsList.get(i);
            oldClaimWorld.getClaims().remove(claim);
        }
        end = System.currentTimeMillis();

        System.out.println("Removed " + amount + " claims with old system in " + (end - start) + "ms");

        //add claims
        start = System.currentTimeMillis();
        for (int i = 0; i < amount; i++) {
            claimWorld.addClaim(claimsList.get(i));
        }
        end = System.currentTimeMillis();
        System.out.println("Added " + amount + " claims in " + (end - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < amount; i++) {
            oldClaimWorld.getClaims().add(claimsList.get(i));
        }
        end = System.currentTimeMillis();

        System.out.println("Added " + amount + " claims with old system in " + (end - start) + "ms");

        System.out.println("Ram usage: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + "MB");
    }

    private Region getRegionWithMaxArea() {
        final Random random = new Random();
        final Region.Point p1 = Region.Point.at(random.nextInt(1000), random.nextInt(4000));
        final Region.Point p2 = Region.Point.at(random.nextInt(1000), random.nextInt(4000));
        return Region.from(p1, p2);
    }

    private Region.Point getRandPoint() {
        final Random random = new Random();
        return Region.Point.at(random.nextInt(4000), random.nextInt(4000));
    }

}
