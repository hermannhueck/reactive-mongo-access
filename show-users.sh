#/bin/sh
#
#	show all documents of the "users" collection
#

mongo << EOF
use shop
db.users.find()
EOF

