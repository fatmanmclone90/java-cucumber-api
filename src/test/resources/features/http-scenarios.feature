Feature: Backend test examples

  Background:
    And I add HTTP headers
      | application-name | EZSystemMS |

  Scenario: Test 1

    Given a request body of
    """json
      {
         "produce": "Cucumbers",
         "weight": "5 Kilo",
         "price": "1€/Kilo",
         "ints": [
           {
            "id": 1
           }
         ]
      }
      """
    Then I print the HTTP request to console
    Then I perform a HTTP POST for route "base/route"
    Then The Http Response code is 200
    And The http response contains JSON Paths
      | $.success | true |

  Scenario: Test 2

    Given A request body of "example.json" with JSON Paths
      | $.currency                                      | USD                           | set            |
      | $.customer.customerEzId                         | ${Configuration:API_BASE_URL} | set            |
      | $.transactions                                  |                               | add_array_item |
      | $.transactions[0].transactionDetails.sale.price | 5.00                          | set            |
    And I set JSON at JSON Path "$.foo"
    """json
        {
           "produce": "Cucumbers",
           "weight": "5 Kilo",
           "price": "1€/Kilo",
           "ints": [
             {
              "id": 1
             }
           ]
        }
    """
    Then I print the HTTP request to console
    Then I perform a HTTP POST for route "base/route"
    Then The Http Response code is 200
    And The http response contains JSON Paths
      | $.success | true |

