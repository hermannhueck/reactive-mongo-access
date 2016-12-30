#/bin/sh
#
#	show all documents of the "orders" collection
#

mongo << EOF
use shop
db.orders.find()
EOF

