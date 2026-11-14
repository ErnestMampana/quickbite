package za.co.ernest.quickbite.kafka.port;

import za.co.ernest.quickbite.entity.User;

public interface TokenGenerator {
    String mintAccessToken(User user);
}
