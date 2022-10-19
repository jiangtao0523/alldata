#pragma once

#if defined(__ELF__) && !defined(OS_FREEBSD)

#include <vector>
#include <string>
#include <unordered_map>
#include <Common/Elf.h>
#include <boost/noncopyable.hpp>

#include <Common/MultiVersion.h>

namespace DB
{

/** Allow to quickly find symbol name from address.
  * Used as a replacement for "dladdr" function which is extremely slow.
  * It works better than "dladdr" because it also allows to search private symbols, that are not participated in shared linking.
  */
class SymbolIndex : private boost::noncopyable
{
protected:
    SymbolIndex() { update(); }

public:
    static MultiVersion<SymbolIndex>::Version instance();
    static void reload();

    struct Symbol
    {
        const void * address_begin;
        const void * address_end;
        const char * name;
    };

    struct Object
    {
        const void * address_begin;
        const void * address_end;
        std::string name;
        std::shared_ptr<Elf> elf;
    };

    /// Address in virtual memory should be passed. These addresses include offset where the object is loaded in memory.
    const Symbol * findSymbol(const void * address) const;
    const Object * findObject(const void * address) const;

    const std::vector<Symbol> & symbols() const { return data.symbols; }
    const std::vector<Object> & objects() const { return data.objects; }

    std::string_view getResource(String name) const
    {
        if (auto it = data.resources.find(name); it != data.resources.end())
            return it->second.data;
        return {};
    }

    /// The BuildID that is generated by compiler.
    String getBuildID() const { return data.build_id; }
    String getBuildIDHex() const;

    struct ResourcesBlob
    {
        /// Symbol can be presented in multiple shared objects,
        /// base_address will be used to compare only symbols from the same SO.
        ElfW(Addr) base_address;
        /// Just a human name of the SO.
        std::string_view object_name;
        /// Data blob.
        std::string_view data;
    };
    using Resources = std::unordered_map<std::string_view /* symbol name */, ResourcesBlob>;

    struct Data
    {
        std::vector<Symbol> symbols;
        std::vector<Object> objects;
        String build_id;

        /// Resources (embedded binary data) are located by symbols in form of _binary_name_start and _binary_name_end.
        Resources resources;
    };
private:
    Data data;

    void update();
    static MultiVersion<SymbolIndex> & instanceImpl();
};

}

#endif