export CY_SCRIPTS_HOME=`realpath "${BASH_SOURCE[0]}"`
export CY_SCRIPTS_HOME=`dirname "$CY_SCRIPTS_HOME"`
cd "$CY_SCRIPTS_HOME/../.."
export CY_SCRIPTS_HOME="`pwd`"

export CY_SOFTWARE_DIR=/opt/software

export VIRTUOSO_DOCKER_ENABLED=true
export VIRTUOSO_UTILS_HOME="$CY_SOFTWARE_DIR/virtuoso-utils"

. config/common-cfg.sh
