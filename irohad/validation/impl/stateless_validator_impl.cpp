/**
 * Copyright Soramitsu Co., Ltd. 2017 All Rights Reserved.
 * http://soramitsu.co.jp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "validation/impl/stateless_validator_impl.hpp"
#include <chrono>
#include <iostream>

namespace iroha {
  namespace validation {
    StatelessValidatorImpl::StatelessValidatorImpl(
        std::shared_ptr<model::ModelCryptoProvider> crypto_provider)
        : crypto_provider_(crypto_provider) {}

    bool StatelessValidatorImpl::validate(
        const model::Transaction& transaction) const {
      std::cout << "StatelessValidator::validate()" << std::endl;
      // signatures are correct
      if (!crypto_provider_->verify(transaction)) return false;
      std::cout << "signatures are correct" << std::endl;

      // time between creation and validation of tx
      uint64_t now = static_cast<uint64_t>(
          std::chrono::duration_cast<std::chrono::milliseconds>(
              std::chrono::system_clock::now().time_since_epoch()).count());

      if (now - transaction.created_ts > MAX_DELAY) {
        return false;
      }

      std::cout << "time between creation and validation of tx" << std::endl;

      // tx is not sent from future
      if (now < transaction.created_ts) {
        return false;
      }

      std::cout << "tx is not sent from future" << std::endl;
      
      return true;
    }

    bool StatelessValidatorImpl::validate(std::shared_ptr<const model::Query> query) const {
      // signatures are correct
      if (!crypto_provider_->verify(query)) return false;

      // time between creation and validation of the query
      uint64_t now = static_cast<uint64_t>(
          std::chrono::duration_cast<std::chrono::milliseconds>(
              std::chrono::system_clock::now().time_since_epoch()).count());

      if (now - query->created_ts > MAX_DELAY) {
        return false;
      }

      // query is not sent from future
      if (now < query->created_ts) {
        return false;
      }
      return true;
    }
  }
}