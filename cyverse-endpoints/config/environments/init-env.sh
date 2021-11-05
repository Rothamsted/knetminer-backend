# These env scripts have to be bash-sourced manually, before doing anything else
#
export CY_SCRIPTS_HOME=`realpath "${BASH_SOURCE[0]}"`
export CY_SCRIPTS_HOME=`dirname "$CY_SCRIPTS_HOME"`
cd "$CY_SCRIPTS_HOME/../.."
export CY_SCRIPTS_HOME="`pwd`"

export CY_SOFTWARE_DIR=/opt/software

export VIRTUOSO_DOCKER_ENABLED=true
export VIRTUOSO_UTILS_HOME="$CY_SOFTWARE_DIR/virtuoso-utils"

# This is common, dataset-independent config and you should have it added to your environment
. config/common-cfg.sh
