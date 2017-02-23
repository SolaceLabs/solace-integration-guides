#!/usr/bin/perl -w
#------------------------------------------------------------------------
# send_ngalert.pl
#    send alert to nagios using NSCA agent
# usage:
#    see usage below
#------------------------------------------------------------------------
use strict ;
use warnings ;
use Sys::Hostname ;
use Getopt::Long ;;

my $me = "send_ngalert.pl" ;
my ($help, $verbose) ;
my $nscadir = "/usr/local/nsca";
my $host = hostname();
my ($service, $rc, $info, $data, $msg) ;
my %statusMap = ("OK" => 0, "UP" => 0,
                 "DOWN" => 1, "WARN" => 1,
		 "UNR" => 2, "UNREACHABLE" => 2, "CRIT" => 2, "CRITICAL" => 2);
#-------------------------------------------------------------------
# process args
#
GetOptions ('nscadir|d=s' => \$nscadir,
	    'host|n=s' => \$host,
	    'status|c=s' => \$rc,
	    'service|s=s' => \$service,
	    'statusinfo|i=s' => \$info,
	    'perfdata|p=s' => \$data,
	    'message|m=s' => \$msg,
	    'help|h' => \$help,
            'verbose|v' => \$verbose);

my ($svrcfg, $clcfg, $send_nsca_bin) = ("$nscadir/cfg/nsca_server.cfg", 
	"$nscadir/cfg/nsca_client.cfg", 
	"$nscadir/bin/send_nsca");

#-------------------------------------------------------------------
# usage
#
if ($help) {
printf "$me : send alert message to Nagios server using NSCA agent

Usage:
  $me [-h] [-v] [-d dir] [-n host] [-s service] -c status -m message 
Options: 
  [-nscadir|d <nsca-dir>]: dir with nsca client installed. 
      /usr/local/nsca by default
  [-host|n <host>]      : host name for alert (localhost by default)
  [-service|s <service> : service for alert.
      sends a service alert if provided.
      sends a host alert if missing
  -status|c <status> : alert status code
     Host Status:
        UP, DOWN, UNR (unreachable)
     Service Status:
        OK, WARN, CRIT (critical)
  -message|m <message> : message to send
  -statusinfo|i <info> : status info  to send
  -perfdata|d   <data> : performance data to send
  -h : help (this message)
  -v : verbose

Payload: 
<message> - [<statusinfo>] perfdata
Pl refer Nagios documentation for format for the payload

Sample:
Send host alert:
$me  -n demo-tr -c OK -i Status=UP -m \"remote host up\"
Send service alert:
$me  -n demo-tr -s remote_alert -c CRIT -i Status=Critical -p '\"Warnings\"=10;10;10 \"Errors\"=3;10;10' -m \"remote service alert\"

";
  exit 1;
}

#-------------------------------------------------------------------
# validate args, env
#
#die "Missing message arg -m" unless $msg ;
die "Missing status-code arg -c" unless $rc ;
die "Unknown status-code $rc" unless exists $statusMap{$rc} ;
die "Error reading server config $svrcfg" unless -e $svrcfg ;
die "Error reading client config $svrcfg" unless -e $clcfg ;

#-------------------------------------------------------------------
# process server cfg 
#
my %svrcfgMap ;
open (SVRCFG, $svrcfg) or die "Error opening server config file $svrcfg" ;
print "Reading server config : $svrcfg\n" ;
while (<SVRCFG>) {
next if /#/;
  my ($tag, $val) = split /=/;
  chop;
  chomp $val ;
  $svrcfgMap{$tag} = $val ;
  print "svrcfgMap{$tag} = $val\n" if $verbose ;
}
close (SVRCFG);

my $status = $statusMap{$rc};
if ($verbose) {
printf " -- input --
nscadir    : $nscadir
server cfg : $svrcfg
client cfg : $clcfg
send_nsca  : $send_nsca_bin
hostname   : $host
service    : $service
status     : $rc ($status)
Message    : ---
Status Info: $info 
Perf Data  : $data
" ;
}



my $cmd ;
my $cmdtype = "host" ;
$msg .= " - [$info] " if $info ;
$msg .= " | $data" if $data ;
if ($service) {
$cmdtype = "service" ;
$cmd = "$host:$service:$status:$msg" ;
}
else {
$cmd = "$host:$status:$msg" ;
}

print "Command    :\n$cmd\n" if $verbose ;

printf "send_nsca command = \"| $send_nsca_bin -H $svrcfgMap{host} -p $svrcfgMap{port} -c $clcfg -d :\"\n" if $verbose ;

printf "starting send_nsca client ...\n" ;
open (SEND_NSCA, "| $send_nsca_bin -H $svrcfgMap{host} -p $svrcfgMap{port} -c $clcfg -d :") || die "Error opening send_nsca ($send_nsca_bin)" ;
print "sending $cmdtype command: $cmd\n" ;
print SEND_NSCA "$cmd\n" ;
while (<SEND_NSCA>) {
 print ;
}

close (SEND_NSCA);