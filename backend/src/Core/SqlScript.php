<?php
declare(strict_types=1);
namespace WebSitePlus\Core;
final class SqlScript
{
    // Migration grammar: SQL statements, quoted literals/identifiers, line comments.
    public static function statements(string $sql): array
    {
        $parts=[]; $buffer=''; $quote=null; $length=strlen($sql);
        for($i=0;$i<$length;$i++) {
            $c=$sql[$i];
            if ($quote !== null) {
                $buffer.=$c;
                if ($c==='\\' && $i+1<$length) { $buffer.=$sql[++$i]; }
                elseif ($c===$quote) {
                    if ($i+1<$length && $sql[$i+1]===$quote) { $buffer.=$sql[++$i]; }
                    else { $quote=null; }
                }
                continue;
            }
            if ($c==='-' && substr($sql,$i,2)==='--') {
                while($i<$length && $sql[$i]!=="\n") { $i++; }
                $buffer.="\n"; continue;
            }
            if (in_array($c,["'",'"','`'],true)) { $quote=$c; $buffer.=$c; }
            elseif ($c===';') { if(trim($buffer)!=='') $parts[]=trim($buffer); $buffer=''; }
            else { $buffer.=$c; }
        }
        if ($quote!==null) { throw new \RuntimeException('Unterminated SQL quote'); }
        if(trim($buffer)!=='') $parts[]=trim($buffer);
        return $parts;
    }
}
