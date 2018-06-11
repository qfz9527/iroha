/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#include <gtest/gtest.h>
#include "framework/integration_framework/integration_test_framework.hpp"
#include "integration/acceptance/acceptance_fixture.hpp"
#include "validators/permissions.hpp"

using namespace integration_framework;
using namespace shared_model;

class SetAccountDetail : public AcceptanceFixture {
 public:
  auto makeUserWithPerms(const std::vector<std::string> &perms = {
                             permissions::can_add_peer}) {
    return AcceptanceFixture::makeUserWithPerms(perms);
  }

  auto baseTx(const interface::types::AccountIdType &account_id,
              const interface::types::AccountDetailKeyType &key,
              const interface::types::AccountDetailValueType &value) {
    return AcceptanceFixture::baseTx().setAccountDetail(account_id, key, value);
  }

  auto baseTx(const interface::types::AccountIdType &account_id) {
    return baseTx(account_id, kKey, kValue);
  }

  auto makeSecondUser(bool use_new_domain = false) {
    static const std::string kUser2 = "user2";
    static const std::string kDomain2 = "test2";

    auto base = TestUnsignedTransactionBuilder()
                    .creatorAccountId(IntegrationTestFramework::kAdminId)
                    .createdTime(getUniqueTime())
                    .quorum(1);
    std::string domain = IntegrationTestFramework::kDefaultDomain;
    if (use_new_domain) {
      domain = kDomain2;
      base = base.createDomain(domain, IntegrationTestFramework::kDefaultRole);
    }
    kUser2Id = kUser2 + "@" + domain;
    return base.createAccount(kUser2, domain, kUser2Keypair.publicKey())
        .build()
        .signAndAddSignature(kAdminKeypair)
        .finish();
  }

  const interface::types::AccountDetailKeyType kKey = "key";
  const interface::types::AccountDetailValueType kValue = "value";
  boost::optional<std::string> kUser2Id;
  const crypto::Keypair kUser2Keypair =
      crypto::DefaultCryptoAlgorithmType::generateKeypair();
};

/**
 * @given a user without can_set_detail permission
 * @when execute tx with SetAccountDetail command aimed to the user
 * @then there is the tx in proposal
 */
TEST_F(SetAccountDetail, Basic) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms())
      .skipProposal()
      .skipBlock()
      .sendTx(complete(baseTx(kUserId)))
      .skipProposal()
      .checkBlock(
          [](auto &block) { ASSERT_EQ(block->transactions().size(), 1); })
      .done();
}

/**
 * @given a pair of users in same domains, first one without permissions
 * @when the first one tries to use SetAccountDetail on the second
 * @then there is the tx in proposal
 */
TEST_F(SetAccountDetail, SameDomainNoPerm) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms())
      .skipProposal()
      .skipBlock()
      .sendTx(makeSecondUser())
      .skipProposal()
      .checkBlock([](auto &block) {
        ASSERT_EQ(block->transactions().size(), 1)
            << "Cannot create second user account";
      })
      .sendTx(complete(baseTx(kUser2Id.value())))
      .skipProposal()
      .checkBlock(
          [](auto &block) { ASSERT_EQ(block->transactions().size(), 0); })
      .done();
}

/**
 * @given a pair of users in same domains, first one with can_set_detail perm
 * @when the first one tries to use SetAccountDetail on the second
 * @then there is the tx in proposal
 */
TEST_F(SetAccountDetail, SameDomain) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms({permissions::can_set_detail}))
      .skipProposal()
      .skipBlock()
      .sendTx(makeSecondUser())
      .skipProposal()
      .checkBlock([](auto &block) {
        ASSERT_EQ(block->transactions().size(), 1)
            << "Cannot create second user account";
      })
      .sendTx(complete(baseTx(kUser2Id.value())))
      .skipProposal()
      .checkBlock(
          [](auto &block) { ASSERT_EQ(block->transactions().size(), 1); })
      .done();
}

/**
 * @given a pair of users in different domains
 * @when the first one tries to use SetAccountDetail on the second
 * @then there is no tx in proposal
 */
TEST_F(SetAccountDetail, DifferentDomain) {
  IntegrationTestFramework(1)
      .setInitialState(kAdminKeypair)
      .sendTx(makeUserWithPerms({permissions::can_set_detail}))
      .skipProposal()
      .skipBlock()
      .sendTx(makeSecondUser(true))
      .skipProposal()
      .checkBlock([](auto &block) {
        ASSERT_EQ(block->transactions().size(), 1)
            << "Cannot create second user account";
      })
      .sendTx(complete(baseTx(kUser2Id.value())), checkStatelessInvalid)
      .done();
}
